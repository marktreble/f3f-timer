function completedRounds(){
	var list = document.createElement('div');
	$(list).addClass('completedrounds list');

	return list;
}

function render_completedrounds(){
    var last_complete_round = model.current_round - 1;
    if (model.race_status == 2) last_complete_round = model.current_round;

	$('#race-name h2').html(model.race_name+" - Results by Round");

	var list = $('div.completedrounds');

	if (last_complete_round < 1){
		$(list).html("<h3>No rounds completed yet</h3>");
		return;
	}

	for (r=1; r<=last_complete_round; r++){
		var li = createListItemLink("Round "+r.toString(), function(){
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
	
	$('#race-name h2').html(model.race_name+" - Round "+(round + 1).toString());

	var list = $('div.completedrounds_results');

	var lis = [];
	var fastest = 999;

    var hli = document.createElement('div');
    $(hli).addClass('list-item');
    lis.push(hli);

	var listHead = document.createElement('span');
	$(listHead).addClass('list-header-rank');
	$(listHead).html("Rank");
	$(hli).append(listHead);
	listHead = document.createElement('span');
	$(listHead).addClass('list-header-pilot-id');
	$(listHead).html("Id");
	$(hli).append(listHead);
	listHead = document.createElement('span');
	$(listHead).addClass('list-header-name');
	$(listHead).html("Name");
	$(hli).append(listHead);
	listHead = document.createElement('span');
	$(listHead).addClass('list-header-right');
	$(listHead).html("Time");
	$(hli).append(listHead);
	listHead = document.createElement('span');
	$(listHead).addClass('list-header-right');
	$(listHead).html("Penalty Points");
	$(hli).append(listHead);
	listHead = document.createElement('span');
	$(listHead).addClass('list-header-right');
	$(listHead).html("Points");
	$(hli).append(listHead);

	var pilots_index_map = new Map();
	for (var i in model.pilots) {
		pilots_index_map.set(model.pilots[i].id, i);
	}

    // create group items
	var group_count = model.racetimes[round].length;
	var group_times = new Array(group_count);
	var group_header_index = 1;
    for (group_index = 0; group_index < group_count; group_index++){
		group_times[group_index] = new Array();
		for (var pilot_index in model.racetimes[round][group_index]){
			var pilot_id = model.racetimes[round][group_index][pilot_index].id;
			var pilot_name = (model.pilots[pilots_index_map.get(pilot_id)].firstname + " " + model.pilots[pilots_index_map.get(pilot_id)].lastname).trim();
			var pilot_penalty = model.racetimes[round][group_index][pilot_index].penalty * 100;
			var pilot_time = model.racetimes[round][group_index][pilot_index].time;
			var pilot_points = model.racetimes[round][group_index][pilot_index].points;
			var pilot_status = model.pilots[pilots_index_map.get(pilot_id)].status;
			var pilot_flown = model.racetimes[round][group_index][pilot_index].flown;
            if (pilot_status == "4") pilot_flown = "0";

			group_times[group_index].push({id:pilot_id, name:pilot_name, time:pilot_time, penalty:pilot_penalty, points:pilot_points, flown:pilot_flown, rank:0});

			var li = document.createElement('div');
			$(li).addClass('list-item');
			lis.push(li);
		}

    	var li = document.createElement('div');
		$(li).addClass('list-item');
		lis.push(li);
		var group = document.createElement('span');
        $(group).addClass('group_header');
        $(group).html("Group " + (group_index+1));
        $(lis[group_header_index]).append(group);

        group_header_index += model.racetimes[round][group_index].length + 1;
    }

	// Loop through groups list, calculate points and add all pilot info to page list
	var group_header_count = 1;
	for (var g in group_times) {
        function comparePoints(a, b) {
            var res;
            var ap = Number(a.points);
            var bp = Number(b.points);
            if (a.flown == "0") return 1;
            if (b.flown == "0") return -1;
            if (Number.isNaN(ap) && !Number.isNaN(bp)) {
                res = 1;
            } else if (!Number.isNaN(ap) && Number.isNaN(bp)) {
                res = -1;
            } else if (Number.isNaN(ap) && Number.isNaN(bp)) {
                res = a.pilot_id - b.pilot_id;
            } else {
                res = bp - ap;
            }
            //console.log("a=%o b=%o ap=%o bp=%o res=%o", a, b, ap, bp, res);
            return res;
        }

        // Sort group by points
        group_times[g].sort(comparePoints);

        // set rank in group
		var rank = 1;
		var last_points = group_times[g][0].points;
		for (var p in group_times[g]) {
			if (group_times[g][p].points != last_points) rank++;
			group_times[g][p].rank = rank;
			last_points = group_times[g][p].points;
        }

		// add all pilot info to page list
		for (var p in group_times[g]) {
			var pilot = group_times[g][p];
			var pilot_index_list = parseInt(p) + group_header_count + 1;

			var pos = document.createElement('span');
			$(pos).addClass('pilot-rank');
			$(pos).html(pilot.rank);
			$(lis[pilot_index_list]).append(pos);

			var pid = document.createElement('span');
			$(pid).addClass('pilot-id');
			$(pid).html(pilot.id);
			$(lis[pilot_index_list]).append(pid);

			var pilot_name = document.createElement('span');
			$(pilot_name).addClass('pilot-name');
			$(pilot_name).html(pilot.name);
			$(lis[pilot_index_list]).append(pilot_name);

			var time = document.createElement('span');
			$(time).addClass('pilot-time');
			$(time).html(pilot.flown == "0" ? "-" : pilot.time + " s");
			$(lis[pilot_index_list]).append(time);

			var penalty = document.createElement('span');
			$(penalty).addClass('pilot-penalty');
			$(penalty).html((pilot.flown == "0" || pilot.penalty == 0) ? "-" : -pilot.penalty);
			$(lis[pilot_index_list]).append(penalty);

			var points = document.createElement('span');
			$(points).addClass('pilot-points');
			$(points).html(parseFloat(pilot.points).toFixed(2));
			$(lis[pilot_index_list]).append(points);
		}
		group_header_count += model.racetimes[round][g].length + 1;
	}

	// Add in flying order not rank
    $(list).empty();
	$(list).append(lis);
}
