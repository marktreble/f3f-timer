function completedRounds(){
	var list = document.createElement('div');
	$(list).addClass('completedrounds list');

	return list;
}

function render_completedrounds(){
	$('#race-name h2').html("Results by Round");

	var list = $('div.completedrounds');

    var last_complete_round = model.current_round - 1;
    if (model.race_status == 2) last_complete_round = model.current_round;

	if (last_complete_round < 1){
		$(list).html("<h3>No rounds completed yet<h3>");
		return;
	}

	for (r=1; r<=last_complete_round; r++){
		li = createListItem("Round "+r.toString(), function(){
			pushView(resultsForRound(), this);
		});

		if ($(list).children().length<r){
			$(list).append(li);
		} else {
			$(list).children().eq(r-1).replaceWith(li);
		}
	}

}

function resultsForRound(){
	var list = document.createElement('div');
	$(list).addClass('completedrounds_results list');

	return list;
}

function render_completedrounds_results(){
	var round = indexPath[indexPath.length-1];

	$('#race-name h2').html("Round "+(round + 1).toString());

	var list = $('div.completedrounds_results');

	var lis = [];
	var times = [];
	var fastest = 999;

	// Loop through pilots, create the html nodes and find the fastest time in the round
	for (i=0; i<model.pilots.length; i++){
		// Create list item html node, and push to the array
		var li = createListItem(responsive_name(model.pilots[i]));
		lis.push(li);

		// Add Pilot's time
		var pilot_time = model.times[round][i];
		var time = document.createElement('span');
		$(time).addClass('pilot-time');
		$(time).html(pilot_time);
		$(li).append(time);

		// Add penalty if applicable
		var pilot_penalty = model.penalties[round][i];
		var penalty = document.createElement('span');
		$(penalty).addClass('pilot-penalty');
		$(penalty).html((pilot_penalty >0) ? "P" : "");
		$(li).append(penalty);

		// If this the round winner?
		if (pilot_time != ""){
			times.push({pilot:i,time:pilot_time,penalty:pilot_penalty})
			pilot_time = parseFloat(pilot_time);
			if (pilot_time>0 && pilot_time<fastest) fastest = pilot_time;
		} else {
			times.push({pilot:i,time:"0",penalty:pilot_penalty})
		}
	}
	// Loop through times and calculate points
	// TODO implement group scoring
    for (var p in times){
		pilot_time = times[p].time;
		penalty = times[p].penalty;
		var points = round2Fixed((fastest/pilot_time*1000)-penalty, 2);
		if (pilot_time == 0) points = 0 - penalty;
		times[p].points = points;
	}

	// Sort list in descending order
	times.sort(function(a, b){return b.points-a.points});

	// Loop through list and add pilot positions and points. Finally add to the list in order
	var position = 1;
	for (var p in times){
		var pos = document.createElement('span');
		$(pos).addClass('pilot-position');
		$(pos).html(position);
		$(lis[times[p].pilot]).append(pos);

		var points = document.createElement('span');
		$(points).addClass('pilot-points');
		$(points).html(times[p].points);
		$(lis[times[p].pilot]).append(points);

		if ($(list).children().length<position){
			$(list).append(lis[times[p].pilot]);
		} else {
			$(list).children().eq(position-1).replaceWith(lis[times[p].pilot]);
		}

		position++
	}

}