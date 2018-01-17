function startlist(){
	var list = document.createElement('div');
	$(list).addClass('startlist list');

	return list;
}

var rowIndexStartlist = 0;
var colIndexStartlist = 0;

function render_startlist(){
	$('#race-name h2').html(model.race_name+" - Startlist");

	var list = $('div.startlist');
    var lis = [];

    var cols1 = [];
    var cols2 = [];
    var cols3 = [];

	var rows1 = [];

	var pilots_index_map = new Map();
	for (var i in model.pilots) {
		pilots_index_map.set(model.pilots[i].id, i);
	}

	// Add html elements
	for (round = 0; round<model.racetimes.length; round++){
        cols1.push("Round " + (round+1));
        cols2.push("Pos");
        cols2.push("Grp");
        cols2.push("Id");
        cols2.push("Name");
    }
    rows1.push(cols1);
    rows1.push(cols2);

	// create pilot items with start_pos and groups
	var i;
	for (round = 0; round<model.racetimes.length; round++){
	    i = 0;
        for (group_index = 0; group_index < model.racetimes[round].length; group_index++){
            var group_size = model.racetimes[round][group_index].length;
            var group_pilots = model.racetimes[round][group_index];

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

            group_pilots.sort(compareStartPos);
            for (pilot_index=0 ; pilot_index<group_pilots.length; pilot_index++){
                var pilot_id = group_pilots[pilot_index].id;
                var pilot_start_pos = group_pilots[pilot_index].start_pos;
                var pilot_status = group_pilots[pilot_index].status;
                var pilots_pilot_index = pilots_index_map.get(String(pilot_id));
                var pilot_name = (model.pilots[pilots_pilot_index].firstname + " " + model.pilots[pilots_pilot_index].lastname).trim();
                var pilot_flown = model.racetimes[round][group_index][pilot_index].flown;
			    if (pilot_flown != "1" && pilot_status == "4") {
					pilot_start_pos = "-";
                }

				var undefinedRow = 0;
				if (rows1[i + 2] === undefined) {
					cols3 = [];
					for (r=0; r<round; r++) {
					    cols3.push("");
					    cols3.push("");
					    cols3.push("");
					    cols3.push("");
					}
					undefinedRow = 1;
				} else {
					if (round > 0) {
						cols3 = rows1[i + 2];
						i++;
					} else {
						cols3 = [];
					}
				}
                cols3.push(pilot_start_pos);
                cols3.push(group_index+1);
                cols3.push(pilot_id);
                cols3.push(pilot_name);
                if (undefinedRow == 1) {
                	rows1.push(cols3);
                	i++;
                } else if (round == 0) {
                    rows1.push(cols3);
                }
            }
        }
    }

	rowIndexStartlist = 0;
    table1 = createTableStartlist(rows1);

    lis.push(table1);

	$(list).empty();
    $(list).html(lis);
}

function createTableStartlist(tableData) {
	if (tableData == "") return;

    var table = document.createElement('table');
    var tableBody = document.createElement('tbody');

    tableData.forEach(function(rowData) {
        var row = document.createElement('tr');
        colIndexStartlist = 0;
        rowData.forEach(function(cellData) {
            var cell = document.createElement('td');
            if (rowIndexStartlist == 0) {
                $(cell).attr('colspan', 4);
                $(cell).addClass('td-border-left');
            }
            if (rowIndexStartlist == 1) {
            	$(cell).addClass('td-border-bottom');
            }
            if ((colIndexStartlist % 4) == 0) {
                $(cell).addClass('td-border-left');
            }
            cell.appendChild(createTableListItemStartlist(cellData, rowIndexStartlist, colIndexStartlist));
            row.appendChild(cell);
            colIndexStartlist++;
        });
        tableBody.appendChild(row);
        rowIndexStartlist++;
    });

    table.appendChild(tableBody);
    return table;
}

function createTableListItemStartlist(cellData, rowIndex, colIndex){
	var span;
	if (rowIndex > 1) {
		if ((colIndex + 1) % 4 == 0) {
	    	span = createSpanStartlist(cellData, 'span-left');
		} else {
			span = createSpanStartlist(cellData, 'span');
		}
    } else {
        span = createHeaderSpanStartlist(cellData);
    }
	return span;
}

function createSpanStartlist(cellData, spanClassName){
	var span = document.createElement('span');
	$(span).addClass(spanClassName);
	$(span).html(cellData);
	return span;
}

function createHeaderSpanStartlist(cellData){
	var span = document.createElement('span');
    $(span).addClass('span-header');
	$(span).html(cellData);
	return span;
}
