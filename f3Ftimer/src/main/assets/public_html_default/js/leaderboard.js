function leaderBoard(){
	var list = document.createElement('div');
	$(list).addClass('leaderboard list');
	
	return list;
}

function render_leaderboard(){
	$('#race-name h2').html("Leader Board");
	
	var list = $('div.leaderboard');
	
	if (model.current_round == 1){
		$(list).html("<h3>No rounds completed yet<h3>");
		return;
	}
	
	var lis = [];
	var total_points = [];
	
	var times = [];
	for (round = 0; round<model.current_round; round++){
		var fastest = 999;

		times.push(round);
		times[round] = [];
		
		// Loop through pilots, and find the fastest time in the round
		for (i=0; i<model.pilots.length; i++){
			
			var pilot_time = model.times[round][i];
			
			var pilot_penalty = model.penalties[round][i];
	
			if (pilot_time != ""){
				times[round].push({pilot:i,time:pilot_time,penalty:pilot_penalty})
				pilot_time = parseFloat(pilot_time);
				if (pilot_time>0 && pilot_time<fastest) fastest = pilot_time;
			}

		}
		
		for (var p in times[round]){
			pilot_time = times[round][p].time;
			penalty = times[round][p].penalty;
			var points = round2Fixed(fastest/pilot_time*1000, 2);
			if (pilot_time == 0) points = 0;
			times[round][p].points = points;
		}
		
	}

	var scores = [];
	var num_discards = (model.current_round>3) ? ((model.current_round>14) ? 2 : 1) : 0;
	for (i=0; i<model.pilots.length; i++){
		// Sort the individual pilot's round, so that discards can be removed
		var pilot_points = [];
		var pilot_penalties = 0;
		for (round = 0; round<model.current_round-1; round++){
			pilot_points.push(times[round][i].points);
			pilot_penalties += parseInt(times[round][i].penalty, 10);
		}
		pilot_points.sort(function(a, b){return b-a});
		
		// Remove discards
		for (var d=0; d<num_discards; d++){
			pilot_points.pop();
		}
		// Total up the remaining
		var tot = 0;
		for (var r in pilot_points){
			tot += parseFloat(pilot_points[r]);
		}
		times[0][i].total = round2Fixed(tot - pilot_penalties, 2);
	}

	times[0].sort(function(a, b){return b.total-a.total});
	
	var position = 1;
	for (var p in times[0]){
		var li = createListItem(responsive_name(model.pilots[times[0][p].pilot]));
	
		var points = document.createElement('span');
		$(points).addClass('pilot-points');
		$(points).html(times[0][p].total);
		$(li).append(points);
		
		if ($(list).children().length<position){
			$(list).append(li);
		} else {
			$(list).children().eq(position-1).replaceWith(li);
		}
		position++;
	}
}