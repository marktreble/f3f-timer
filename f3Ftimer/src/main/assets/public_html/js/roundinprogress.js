function roundInProgress(){
	var list = document.createElement('div');
	$(list).addClass('roundinprogress list');
	
	return list;
}

function render_roundinprogress(){
	$('#race-name h2').html("In Progress (R"+model.current_round+")");
	
	var list = $('div.roundinprogress');
	
	var lis = [];
	var times = [];
	var fastest = 999;

	// Loop through pilots, create the html nodes and find the fastest time in the round
	for (i=0; i<model.pilots.length; i++){
		var li = createListItem(responsive_name(model.pilots[i]));
		lis.push(li);
		
		var pilot_time = model.times[model.current_round-1][i];
		
		var pilot_penalty = model.penalties[model.current_round-1][i];

		// If this the round winner so far?
		if (pilot_time != ""){
			times.push({pilot:i,time:pilot_time,penalty:pilot_penalty})
			pilot_time = parseFloat(pilot_time);
			if (pilot_time>0 && pilot_time<fastest) fastest = pilot_time;
		}
	}
	
	// Loop through times and calculate points
	for (var p in times){
		pilot_time = times[p].time;
		penalty = times[p].penalty;
		var points = Math.round(fastest/pilot_time*1000) - penalty;
		if (pilot_time == 0) points = 0 - penalty;
		times[p].points = points;
	}
	
	times.sort(function(a, b){return b.points-a.points});

	// Loop through list and add pilot positions and points
	var position = 1;
	for (var p in times){
		var pos = document.createElement('span');
		$(pos).addClass('pilot-position');
		$(pos).html(position++);
		$(lis[times[p].pilot]).append(pos);

		var points = document.createElement('span');
		$(points).addClass('pilot-points');
		$(points).html(times[p].points);
		$(lis[times[p].pilot]).append(points);
		
		var penalty = document.createElement('span');
		$(penalty).addClass('pilot-penalty');
		$(penalty).html((times[p].penalty >0) ? "P" : "");
		$(lis[times[p].pilot]).append(penalty);
		
		var time = document.createElement('span');
		$(time).addClass('pilot-time');
		$(time).html(times[p].time);
		$(lis[times[p].pilot]).append(time);
	}
	
	// Add in flying order not position
	position = 1;
	for(var i in lis){
		
		if ($(list).children().length<position){
			$(list).append(lis[i]);
		} else {
			$(list).children().eq(i).replaceWith(lis[i]);
		}
		position++;
	}
}