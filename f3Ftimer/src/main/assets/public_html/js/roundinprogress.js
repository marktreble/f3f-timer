function roundInProgress(){
	var list = document.createElement('div');
	$(list).addClass('roundinprogress list');
	
	return list;
}

function render_roundinprogress(){
	var round = model.current_round - 1;

	$('#race-name h2').html(model.race_name+" - Round in Progress (Round "+model.current_round+")");
	
	var list = $('div.roundinprogress');

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
	$(listHead).html("Pen.");
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
			var pilot_start_pos = model.racetimes[round][group_index][pilot_index].start_pos;
			var pilot_id = model.racetimes[round][group_index][pilot_index].id;
			var pilot_name = (model.pilots[pilots_index_map.get(pilot_id)].firstname + " " + model.pilots[pilots_index_map.get(pilot_id)].lastname).trim();
			var pilot_penalty = model.racetimes[round][group_index][pilot_index].penalty * 100;
			var pilot_time = model.racetimes[round][group_index][pilot_index].time;
			var pilot_points = model.racetimes[round][group_index][pilot_index].points;
			var pilot_status = model.racetimes[round][group_index][pilot_index].status;
			var flown = model.racetimes[round][group_index][pilot_index].flown;
			if (flown == "0" || pilot_status == "4") {
				pilot_time = "-";
				pilot_penalty = "-";
				pilot_points = "-";
			}
			group_times[group_index].push({rank:0,start_pos:pilot_start_pos,id:pilot_id,name:pilot_name,time:pilot_time,penalty:pilot_penalty,points:pilot_points});

			var li = document.createElement('div');
			$(li).addClass('list-item');
			lis.push(li);
		}

		function comparePoints(a, b) {
			var res;
			var ap = Number(a.points);
			var bp = Number(b.points);
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

		group_times[group_index].sort(comparePoints)

		var rank = 1;
		var last_points = group_times[group_index][0].points;
		for (var p in group_times[group_index]) {
			var pilot = group_times[group_index][p];
			if (pilot.points != last_points) rank++;
			pilot.rank = pilot.time != "-" ? rank : "-";
			last_points = pilot.points;
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
        function compareStartPos(a, b) {
            var res;
            var ap = Number(a.start_pos);
            var bp = Number(b.start_pos);
            if (Number.isNaN(ap) && !Number.isNaN(bp)) {
                res = -1;
            } else if (!Number.isNaN(ap) && Number.isNaN(bp)) {
                res = 1;
            } else if (Number.isNaN(ap) && Number.isNaN(bp)) {
                res = a.pilot_id - b.pilot_id;
            } else {
                if (ap == 0) res = 1;
                else if (bp == 0) res = -1;
                else res = ap - bp;
            }
            //console.log("start_pos: a=%o b=%o ap=%o bp=%o res=%o", a, b, ap, bp, res);
            return res;
        }

		group_times[g].sort(compareStartPos)

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
			$(time).html(pilot.time + (pilot.time != "-" ? "s" : ""));
			$(lis[pilot_index_list]).append(time);

			var penalty = document.createElement('span');
			$(penalty).addClass('pilot-penalty');
			$(penalty).html(pilot.penalty);
			$(lis[pilot_index_list]).append(penalty);

			var points = document.createElement('span');
			$(points).addClass('pilot-points');
			$(points).html(pilot.points);
			$(lis[pilot_index_list]).append(points);
		}
		group_header_count += model.racetimes[round][g].length + 1;
	}

	// add sorted list to page
    $(list).empty();
	$(list).append(lis);
}
