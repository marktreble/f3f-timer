package com.marktreble.f3ftimer.resultsmanager;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.marktreble.f3ftimer.R;
import com.marktreble.f3ftimer.data.pilot.Pilot;
import com.marktreble.f3ftimer.data.race.Race;
import com.marktreble.f3ftimer.data.race.RaceData;
import com.marktreble.f3ftimer.data.racepilot.RacePilotData;
import com.marktreble.f3ftimer.data.results.Results;
import com.marktreble.f3ftimer.dialog.AboutActivity;
import com.marktreble.f3ftimer.dialog.HelpActivity;
import com.marktreble.f3ftimer.filesystem.SpreadsheetExport;
import com.marktreble.f3ftimer.pilotmanager.PilotsActivity;
import com.marktreble.f3ftimer.racemanager.RaceListActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;


public class ResultsRaceActivity extends ListActivity {

	private ArrayAdapter<String> mArrAdapter;

	private Integer mRid;

	static final boolean DEBUG = true;

	private Context mContext;

	private AlertDialog mDlg;
	private AlertDialog.Builder mDlgb;

	static final int EXPORT_EMAIL = 0;
	static final int EXPORT_F3F_TIMER = 1;
	static final int EXPORT_F3X_VAULT = 2;


	static final int SHARE_EMAIL = 0;
	static final int SHARE_SOCIAL_MEDIA = 1;

	/* TEMPORARY */
	// Needs a class creating for calcs
	private ArrayList<String> mArrNames;
	private ArrayList<String> mArrNumbers;
	private ArrayList<Pilot> mArrPilots;
	private ArrayList<Float> mArrScores;

	private float mFTD;
	private String mFTDName;
	private int mFTDRound;
	private RaceData.Group mGroupScoring;
	/* END */


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		ImageView view = (ImageView)findViewById(android.R.id.home);
		Resources r = getResources();
		int px = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, r.getDisplayMetrics());
		view.setPadding(0, 0, px, 0);

		mContext = this;

		setContentView(R.layout.race);
	    
		Intent intent = getIntent();
		if (intent.hasExtra("race_id")){
			Bundle extras = intent.getExtras();
			mRid = extras.getInt("race_id");
		}
		
		RaceData datasource = new RaceData(this);
  		datasource.open();
  		Race race = datasource.getRace(mRid);
  		datasource.close();	
  		
 		TextView tt = (TextView) findViewById(R.id.race_title);
  		tt.setText(race.name);
  		
		ArrayList<String> arrOptions = new ArrayList<String>();
		arrOptions.add(String.format("Round in Progress (R%d)", race.round));
		arrOptions.add("Completed Rounds");
		arrOptions.add("Leader Board");
		arrOptions.add("Team Results");

   	   	mArrAdapter = new ArrayAdapter<String>(this, R.layout.listrow , arrOptions);
        setListAdapter(mArrAdapter);        
	}
	
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		Intent intent = null;
		switch (position){
			case 0:
				 intent = new Intent(this, ResultsRoundInProgressActivity.class);
				 break;
			case 1:
				 intent = new Intent(this, ResultsCompletedRoundsActivity.class);				
				break;
			case 2:
				 intent = new Intent(this, ResultsLeaderBoardActivity.class);
				break;
			case 3:
				 intent = new Intent(this, ResultsTeamsActivity.class);
				break;
		}
        intent.putExtra("race_id", mRid);
    	startActivityForResult(intent, mRid);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.results, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle presses on the action bar items
	    switch (item.getItemId()) {
			case R.id.menu_share:
				share();
				return true;
			case R.id.menu_export:
				export();
				return true;
			case R.id.menu_pilot_manager:
				pilotManager();
				return true;
			case R.id.menu_race_manager:
				raceManager();
				return true;
			case R.id.menu_help:
				help();
				return true;
			case R.id.menu_about:
				about();
				return true;


			default:
				return super.onOptionsItemSelected(item);
	    }
	}

	private void share() {

		mDlgb = new AlertDialog.Builder(mContext)
				.setTitle(R.string.select_share_results_destination)
				.setItems(R.array.results_share_destinations, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						switch (which) {
							case SHARE_EMAIL:
								share_email();
								break;
							case SHARE_SOCIAL_MEDIA:
								share_social_media();
								break;
						}
					}
				});
		mDlg = mDlgb.create();
		mDlg.show();
	}

	private void share_email(){
		RaceData datasource = new RaceData(this);
		datasource.open();
		Race race = datasource.getRace(mRid);
		datasource.close();

		this.getNamesArray();

		String results = "";
		String[] email_list = new String[mArrNames.size()];

		for (int i=0; i<mArrNames.size(); i++){
			results+= String.format("%s %s %s\n", mArrNumbers.get(i), mArrNames.get(i), Float.toString(mArrScores.get(i)));

			// Generate list of email addresses to send to
			Pilot p = mArrPilots.get(i);
			String email = p.email;
			if (email.length()>0)
				email_list[i] = email;

		}

		results+= "\n";
		results+= "Fastest time: " + mFTD + " by " + mFTDName + " in round " + mFTDRound + "\n";
		results+= "\n";
		results+= "\n\n";
		results+= "Result from f3ftimer (https://github.com/marktreble/f3f-timer)\n";

		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.setType("message/rfc822");
		intent.putExtra(Intent.EXTRA_EMAIL, email_list);
		intent.putExtra(Intent.EXTRA_SUBJECT, race.name);
		intent.putExtra(Intent.EXTRA_TEXT, results);

		Intent openInChooser = Intent.createChooser(intent, "Share Leaderboard");
		startActivityForResult(openInChooser, 0);
	}

	private void share_social_media(){
		RaceData datasource = new RaceData(this);
		datasource.open();
		Race race = datasource.getRace(mRid);
		datasource.close();

		this.getNamesArray();

		// Generate results as an image
		int w = 320, h = (mArrNames.size()+6) * 24;
		Bitmap.Config conf = Bitmap.Config.ARGB_8888;
		Bitmap bitmap = Bitmap.createBitmap(w, h, conf);
		Canvas canvas = new Canvas(bitmap);
		Paint paint = new Paint();
		paint.setColor(Color.WHITE);
		canvas.drawRect(0,0,w,h, paint);
		paint.setColor(Color.BLACK);
		paint.setTextSize(18);
		paint.setAntiAlias(true);
		paint.setFilterBitmap(true);

		int y = 30;
		for (int i=0; i<mArrNames.size(); i++){
			y += 24;
			canvas.drawText(mArrNumbers.get(i), 16, y, paint);
			canvas.drawText(mArrNames.get(i), 48, y, paint);
			canvas.drawText(Float.toString(mArrScores.get(i)), 220, y, paint);

		}
		y += 48;

		canvas.drawText("Fastest time: " + mFTD, 16, y, paint);
		y+=20;
		canvas.drawText("by " + mFTDName + " in round " + mFTDRound, 16, y, paint);

		Intent intent = new Intent(Intent.ACTION_SEND);

		try {
			File file = new File(getExternalCacheDir(), race.name+".png");
			FileOutputStream fOut = new FileOutputStream(file);
			bitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut);
			fOut.flush();
			fOut.close();
			file.setReadable(true, false);
			intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
			intent.putExtra(Intent.EXTRA_TEXT, "Result from f3ftimer (https://github.com/marktreble/f3f-timer)");

			intent.setType("image/png");
		} catch (Exception e) {
			e.printStackTrace();
		}

		Intent openInChooser = Intent.createChooser(intent, "Share Leaderboard");

		startActivity(openInChooser);
	}

	private void export(){
		mDlgb = new AlertDialog.Builder(mContext)
				.setTitle(R.string.select_export_results_destination)
				.setItems(R.array.results_export_destinations, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						switch (which) {
							case EXPORT_EMAIL:
								export_email();
								break;
							case EXPORT_F3F_TIMER:
								export_f3ftimer();
								break;
							case EXPORT_F3X_VAULT:
								export_f3xvault();
								break;
						}
					}
				});
		mDlg = mDlgb.create();
		mDlg.show();
	}

	private void pilotManager(){
		Intent intent = new Intent(mContext,PilotsActivity.class);
		startActivity(intent);
	}

	private void raceManager(){
		Intent intent = new Intent(mContext, RaceListActivity.class);
		startActivity(intent);
	}

	private void help(){
		Intent intent = new Intent(mContext, HelpActivity.class);
		startActivity(intent);
	}

	private void about(){
		Intent intent = new Intent(mContext, AboutActivity.class);
		startActivity(intent);
	}

	private void export_email(){
		RaceData datasource = new RaceData(this);
		datasource.open();
		Race race = datasource.getRace(mRid);
		datasource.close();

		// re-write the results file just in case this has just been imported into this device.
		new SpreadsheetExport().writeResultsFile(mContext, race);


		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.setType("message/rfc822");
		intent.putExtra(Intent.EXTRA_SUBJECT, race.name);
		intent.putExtra(Intent.EXTRA_TEXT, "Results file attached");

		File file = new SpreadsheetExport().getDataStorageDir(race.name+".txt");
		if (!file.exists() || !file.canRead()) {
			Toast.makeText(this, "Attachment Error", Toast.LENGTH_SHORT).show();
			finish();
			return;
		}
		Uri uri = Uri.fromFile(file);
		intent.putExtra(Intent.EXTRA_STREAM, uri);

		Intent openInChooser = Intent.createChooser(intent, "Email Results File");
		startActivity(openInChooser);
	}

	private void export_f3ftimer(){
		mDlgb = new AlertDialog.Builder(mContext)
				.setTitle("TO DO...")
				.setMessage("This feature will be implemented soon")
				.setNegativeButton("Ok", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						//
					}
				});


		mDlg = mDlgb.create();
		mDlg.show();

	}

	private void export_f3xvault(){
		mDlgb = new AlertDialog.Builder(mContext)
				.setTitle("TO DO...")
				.setMessage("This feature will be implemented soon")
				.setNegativeButton("Ok", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						//
					}
				});


		mDlg = mDlgb.create();
		mDlg.show();
	}

	private void getNamesArray(){

		RaceData datasource = new RaceData(ResultsRaceActivity.this);
		datasource.open();
		Race race = datasource.getRace(mRid);

		RacePilotData datasource2 = new RacePilotData(ResultsRaceActivity.this);
		datasource2.open();
		ArrayList<Pilot> allPilots = datasource2.getAllPilotsForRace(mRid, 1);
		ArrayList<String> p_names = new ArrayList<>();
		ArrayList<String> p_emails = new ArrayList<>();
		ArrayList<String> p_bib_numbers = new ArrayList<>();
		ArrayList<String> p_nationalities = new ArrayList<>();
		ArrayList<float[]> p_times = new ArrayList<>();
		ArrayList<float[]> p_points = new ArrayList<>();
		ArrayList<int[]> p_penalty = new ArrayList<>();
		Float[] p_totals;
		int[] p_positions;

		mFTD = 9999;

		if (allPilots != null){

			// Get all times for pilots in all rounds
			int c=0; // Counter for bib numbers
			for (Pilot p : allPilots){
				if (p.pilot_id>0) {
					p_names.add(String.format("%s %s", p.firstname, p.lastname));
					p_emails.add(p.email);
					p_bib_numbers.add(Integer.toString(c + 1));
					p_nationalities.add(p.nationality);
					float[] sc = new float[race.round];
					for (int rnd = 0; rnd < race.round; rnd++) {
						sc[rnd] = datasource2.getPilotTimeInRound(mRid, p.id, rnd + 1);
					}
					p_times.add(sc);
					Log.d("LEADERBOARD", String.format("%s %s %d", p.firstname, p.lastname, c+1));
				}
				c++;
			}

			p_totals = new Float[p_names.size()];
			p_positions = new int[p_names.size()];

			if (race.round>1){
				// Loop through each round to find the winner, then populate the scores
				for (int rnd=0; rnd<race.round-1; rnd++){
					ArrayList<Pilot> pilots_in_round = datasource2.getAllPilotsForRace(mRid, rnd+1);

					mGroupScoring = datasource.getGroups(mRid, rnd+1);

					int g = 0; // Current group we are calculating

					float[] ftg = new float[mGroupScoring.num_groups+1]; // Fastest time in group (used for calculating normalised scores)
					for (int i=0; i<mGroupScoring.num_groups+1; i++)
						ftg[i]= 9999;

					int group_size = (int)Math.floor(p_names.size()/mGroupScoring.num_groups);
					int remainder = p_names.size() - (mGroupScoring.num_groups * group_size);

					for (int i=0; i<p_names.size(); i++){
						if (g<remainder){
							if (i>= (group_size+1)*(g+1)) {
								g++;
							}
						} else {
							if (i>= ((group_size+1)*remainder) + (group_size*((g+1)-remainder))) {
								g++;
							}
						}

						String str_t = String.format("%.2f",p_times.get(i)[rnd]).trim().replace(",", ".");
						float t = Float.parseFloat(str_t);
						if (t>0)
							ftg[g] = Math.min( t, ftg[g]);

						// Update the FTD here too
						mFTD = Math.min(mFTD,  ftg[g]);
						if (mFTD == t){
							mFTDRound = rnd+1;
							mFTDName = p_names.get(i);
						}

					}

					g = 0; // Current group we are calculating

					float[] points = new float[p_names.size()];
					int[] penalty = new int[p_names.size()];
					for (int i=0; i<p_names.size(); i++){
						if (g<remainder){
							if (i>= (group_size+1)*(g+1)) {
								g++;
							}
						} else {
							if (i>= ((group_size+1)*remainder) + (group_size*((g+1)-remainder))) {
								g++;
							}
						}

						String str_t = String.format("%.2f",p_times.get(i)[rnd]).trim().replace(",", ".");
						float time = Float.parseFloat(str_t);
						float pnts = 0;
						if (time>0)
							pnts = Results.round2Fixed((ftg[g]/time) * 1000, 2);


						points[i] = pnts;
						penalty[i] = pilots_in_round.get(i).penalty;
					}
					p_points.add(points);
					p_penalty.add(penalty);
				}

				// Loop through each pilot to Find discards + calc totals
				int numdiscards = (race.round>4) ? ((race.round>15) ? 2 : 1) : 0;
				for (int i=0; i<p_names.size(); i++){
					Float[] totals = new Float[race.round-1];

					float penalties = 0;
					for (int rnd=0; rnd<race.round-1; rnd++){
						totals[rnd] = p_points.get(rnd)[i];
						penalties+=p_penalty.get(rnd)[i] * 100;
					}

					// sort totals in order then lose the lowest according to numdiscards
					Arrays.sort(totals);
					float tot = 0;
					for (int j=numdiscards; j<race.round-1; j++)
						tot += totals[j];

					// Now apply penalties
					p_totals[i] = tot - penalties;
				}

				// Now sort the pilots
				Float[] p_sorted_totals = p_totals.clone();
				Arrays.sort(p_sorted_totals, Collections.reverseOrder());

				// Set the positions according to the sorted order
				for (int i=0; i<p_names.size(); i++){
					for (int j=0; j<p_names.size(); j++){
						if (p_totals[i] == p_sorted_totals[j])
							p_positions[i] = j + 1;

					}
				}

				int sz = p_names.size();
				mArrNames = new ArrayList<>(sz);
				mArrNumbers = new ArrayList<>(sz);
				mArrPilots = new ArrayList<>(sz);
				mArrScores = new ArrayList<>(sz);

				// Initialise
				for (int i = 0; i < sz; i++) {
					mArrNames.add("");
					mArrNumbers.add("");
					mArrPilots.add(new Pilot());
					mArrScores.add(1000f);
				}

				for (int i=0; i<sz; i++){
					int pos = p_positions[i]-1;
					mArrNames.set(pos, String.format("%s", p_names.get(i)));
					//mArrNumbers.set(pos, p_bib_numbers.get(i));
					mArrNumbers.set(pos, Integer.toString(p_positions[i]));
					Pilot p = new Pilot();
					p.points = Results.round2Fixed(p_totals[i].floatValue(), 2);
					p.nationality = p_nationalities.get(i);
					p.email = p_emails.get(i);
					mArrPilots.set(pos, p);
				}

				float top_score = mArrPilots.get(0).points;
				float previousscore = 1000.0f;

				int pos = 1, lastpos = 1; // Last pos is for ties
				for (int i=1; i<sz; i++){
					float pilot_points = mArrPilots.get(i).points;
					float normalised = Results.round2Fixed(pilot_points/top_score * 1000, 2);

					previousscore = normalised;

					mArrScores.set(i, Float.valueOf(normalised));
				}
			} else {
				// No rounds complete yet
				mArrNames = new ArrayList<String>(0);
				mArrPilots = new ArrayList<Pilot>(0);
				mArrScores = new ArrayList<Float>(0);
			}
			datasource2.close();
		}
		datasource.close();
	}
}
