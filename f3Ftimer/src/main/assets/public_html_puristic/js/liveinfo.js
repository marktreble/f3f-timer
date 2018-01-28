function liveInfo(){
	var list = document.createElement('div');
	list.classList.add('liveinfo');
	list.classList.add('list');

	return list;
}

function render_liveinfo(){
    var cols1 = [];
    var cols2 = [];
    var cols3 = [];
    var cols4 = [];
    var cols5 = [];
    var cols6 = [];
    var cols7 = [];
    var cols8 = [];
    var cols9 = [];
    var cols10 = [];
    var cols11 = [];

	var rows1 = [];
	var rows2 = [];
	var rows3 = [];

    var topHeading = document.getElementById('race-name').children[1];
    topHeading.innerHTML = modelLive.race_name+" - Live Info (Round "+modelLive.current_round+")";

    cols1 = "#Abs#Rel#Speed#Status";
    cols1 = cols1.split("#");
	rows1.push(cols1);
    if (modelLive.current_wind_legal == "true" && parseInt(modelLive.current_wind_speed_counter) == 20) {
        cols1 = "Wind:#" + modelLive.current_wind_angle_absolute+" °#"+modelLive.current_wind_angle_relative+" °#"+modelLive.current_wind_speed+" m/s#legal";
    } else if (modelLive.current_wind_legal == "true") {
        cols1 = "Wind:#" + modelLive.current_wind_angle_absolute+" °#"+modelLive.current_wind_angle_relative+" °#"+modelLive.current_wind_speed+" m/s#illegal ("+modelLive.current_wind_speed_counter+"s)";
    } else {
        cols1 = "Wind:#" + modelLive.current_wind_angle_absolute+" °#"+modelLive.current_wind_angle_relative+" °#"+modelLive.current_wind_speed+" m/s#illegal";
    }
    cols1 = cols1.split("#");
	rows1.push(cols1);

    if (modelLive.state >= 1) {
        cols2 = [ "Pilot: ", modelLive.current_pilot ];
        rows2.push(cols2);
    }
    if (modelLive.state >= 2) {
        cols3 = [ "Working Time: ", modelLive.current_working_time ];
        rows2.push(cols3);
    }
    if (modelLive.state >= 3) {
        cols3.push("Launched");
    }
    if (modelLive.state >= 3) {
        cols4 = [ "Climb Out Time: ", modelLive.current_climb_out_time ];
        rows2.push(cols4);
    }
    if (modelLive.state == 4) {
        cols4.push("Off Course");
    }
    if (modelLive.state >= 5) {
        cols4.push("On Course");
        cols5 = [ "Flight Time: ", modelLive.current_flight_time ];
        rows2.push(cols5);
        cols6 = [ "Penalty Points: ", modelLive.current_penalty * 100 ];
        rows2.push(cols6);
        if (modelLive.current_turn_numbers !== undefined) {
		    cols7 = ("#"+modelLive.current_turn_numbers).split("#");
		    rows3.push(cols7);
            cols8 = ("Leg:#"+modelLive.current_split_times).split("#");
            rows3.push(cols8);
            cols9 = ("Fastest:#"+modelLive.fastest_times).split("#");
            rows3.push(cols9);
            cols10 = ("Delta:#"+modelLive.delta_times).split("#");
            rows3.push(cols10);
			if (cols7.length < 11 && modelLive.current_estimated_flight_time > 0) {
				cols11 = [ "Estimated: " ];
				for (var i = 1; i < cols7.length; i++) {
					cols11.push("");
				}
				cols11.push(modelLive.current_estimated_flight_time);
				rows3.push(cols11);
			}
            cols7.push("Overall");
            var n = modelLive.current_split_times.split("#").reduce(function(a, b) { return parseFloat(a) + parseFloat(b); }, 0).toFixed(2);
            cols8.push(n);
			if (cols7.length >= 12) {
			    cols9.push(modelLive.fastest_time);
			    var n = (modelLive.current_flight_time - modelLive.fastest_time).toFixed(2);
			    cols10.push((n>0?'+':'') + n);
                cols9[0] = "Fastest (" + modelLive.fastest_time_pilot.trim() + "):";
            } else {
            	var n = modelLive.fastest_times.split("#").reduce(function(a, b) { return parseFloat(a) + parseFloat(b); }, 0).toFixed(2);
                cols9.push(n);
                n = (cols8[cols8.length - 1] - cols9[cols9.length - 1]).toFixed(2);
                cols10.push((n>0?'+':'') + n);
                cols9[0] = "Fastest (" + modelLive.fastest_time_pilot.trim() + "):";
            }
        }
    }
    if (modelLive.state >= 6) {
        cols5.push("Flight finished");
    }

    var list = document.getElementsByClassName('liveinfo list')[0];
	list.innerHTML = "";

    table1 = createTableLive(rows1, 1);
    list.appendChild(table1);

	var width2 = 0;
	if (rows2.length > 0) {
		table2 = createTableLive(rows2, 2);
		width2 = table2.rows[0].cells[0].offsetWidth;
		list.appendChild(document.createElement('br'));
		list.appendChild(table2);
	}
	var width3 = 0;
	if (rows3.length > 0) {
		table3 = createTableLive(rows3, 3);
		width3 = table3.rows[0].cells[0].offsetWidth;
		list.appendChild(document.createElement('br'));
		list.appendChild(table3);
	}

	// adjust first column width of tables
	var max = Math.max(width2, width3);
	if (max > 0) {
		table1.rows[0].cells[0].width = max;
		table2.rows[0].cells[0].width = max;
		table3.rows[0].cells[0].width = max;
	}
}

function createTableLive(tableData, tableNo) {
	if (tableData == "") return;

    var table = document.createElement('table');
    var tableBody = document.createElement('tbody');

    for (var rowIndexLive = 0; rowIndexLive<tableData.length; rowIndexLive++) {
        var row = document.createElement('tr');
        for (var colIndexLive = 0; colIndexLive<tableData[rowIndexLive].length; colIndexLive++) {
            var cell = document.createElement('td');
            cell.appendChild(createListItemLive(tableData[rowIndexLive][colIndexLive], rowIndexLive, colIndexLive, tableNo));
            row.appendChild(cell);
        }
        tableBody.appendChild(row);
    }

    table.appendChild(tableBody);
    return table;
}

function createListItemLive(cellData, rowIndexLive, colIndexLive, tableNo){
	var span;
	if ((tableNo == 2 && colIndexLive > 0) ||
		(tableNo != 2 && colIndexLive > 0 && rowIndexLive > 0)) {
	    span = createSpanLiveLegs(cellData, rowIndexLive, colIndexLive, tableNo);
    } else {
	    span = createSpan(cellData, 'span-live-header');
    }
	return span;
}

function createSpanLiveLegs(cellData, rowIndexLive, colIndexLive, tableNo){
	var span = document.createElement('span');
	if (tableNo == 3 && rowIndexLive == 3 && colIndexLive > 0) {
        if (cellData < 0) {
            span.classList.add('span-live-legs-neg');
        } else {
            span.classList.add('span-live-legs-pos');
        }
	} else {
	    span.classList.add('span-live-legs');
    }
	span.innerHTML = cellData;
	return span;
}
