function teamresults(){
	var list = document.createElement('div');
	$(list).addClass('teamresults list');

	return list;
}

function render_teamresults(){
    var last_complete_round = model.current_round - 1;
    if (model.race_status == 2) last_complete_round = model.current_round;

	var first_discard_round = 4;
	var second_discard_round = 15;
	var num_discards = (last_complete_round>=first_discard_round) ? ((last_complete_round>=second_discard_round) ? 2 : 1) : 0;

	$('#race-name h2').html(model.race_name+" - Team Results <br>(Completed Rounds: "+last_complete_round+", Discarded Rounds: "+num_discards+")");

	var list = $('div.teamresults');
    var lis = [];

	if (last_complete_round < 1){
		$(list).html("<h3>No rounds completed yet</h3>");
		return;
	}

	// get final score
	var sub_totals_final_teamresults = subTotals(last_complete_round);

	// get teams
	var sub_totals_final_teams = [];
	for (pilot_index = 0; pilot_index<sub_totals_final_teamresults.length; pilot_index++){
		var team = sub_totals_final_teamresults[pilot_index].team;
		if (team != "" && -1 == sub_totals_final_teams.indexOf(team))
			sub_totals_final_teams.push(team);
	}

    if (sub_totals_final_teams.length < 1){
        $(list).html("<h3>No teams defined</h3>");
        return;
    }

    // calc team score
    for (team_index = 0; team_index<sub_totals_final_teams.length; team_index++){
        var team_points = 0.0;
        var count = 0;
        for (pilot_index = 0; pilot_index<sub_totals_final_teamresults.length; pilot_index++){
            if (count < 3 && sub_totals_final_teamresults[pilot_index].team == sub_totals_final_teams[team_index]) {
                team_points += sub_totals_final_teamresults[pilot_index].total;
                count++;
            }
        }
        sub_totals_final_teams[team_index] = {team:sub_totals_final_teams[team_index], points:team_points, percent:0.0};
    }

    // sort team results
    sub_totals_final_teams.sort(function(a, b){return b.points-a.points});

    // calc team percent
    for (team_index = 0; team_index<sub_totals_final_teams.length; team_index++){
        sub_totals_final_teams[team_index].percent = round2Fixed((sub_totals_final_teams[team_index].points / sub_totals_final_teams[0].points) * 100, 2);
    }

    var cols1 = [];
    var cols2 = [];
    var cols3 = [];

	var rows1 = [];
	var rows2 = [];

	// add header
	cols1.push("Rank");
	cols1.push("Team");
	cols1.push("Pts");
	cols1.push("%");
	cols1.push("Pilots");
	rows1.push(cols1);

	// Add html elements
	for (team_index = 0; team_index<sub_totals_final_teams.length; team_index++){
		cols1 = [];
		cols1.push(team_index + 1);
		cols1.push(sub_totals_final_teams[team_index].team);
		cols1.push(sub_totals_final_teams[team_index].points.toFixed(2));
		cols1.push(sub_totals_final_teams[team_index].percent.toFixed(2));

		// get the team pilots
		rows2 = [];
        cols2 = [];

		cols2.push("Pos");
		cols2.push("Id");
		cols2.push("Name");
		cols2.push("Pts");
		cols2.push("%");
		rows2.push(cols2);

		for (pilot_index = 0; pilot_index<sub_totals_final_teamresults.length; pilot_index++){
			if (sub_totals_final_teamresults[pilot_index].team == sub_totals_final_teams[team_index].team) {
				cols3 = [];
				cols3.push(sub_totals_final_teamresults[pilot_index].rank);
				cols3.push(sub_totals_final_teamresults[pilot_index].pilot_id);
				cols3.push(sub_totals_final_teamresults[pilot_index].pilot_name);
				cols3.push(sub_totals_final_teamresults[pilot_index].total.toFixed(2));
				cols3.push(sub_totals_final_teamresults[pilot_index].percent.toFixed(2));
				rows2.push(cols3);
			}
		}
		table1 = createTableTeamResults(rows2, 2);
		cols1.push(table1);
		rows1.push(cols1);
    }

	table2 = createTableTeamResults(rows1, -1);
    lis.push(table2);
	$(list).empty();
    $(list).html(lis);

	// adjust column width of tables
	var max_cell_width = [];
	for (table_index = 1; table_index<rows1.length; table_index++){
		var table = table2.rows[table_index].cells[4].firstChild;
		for (col_index = 0; col_index<table.rows[0].cells.length; col_index++){
			var cell_width = table.rows[0].cells[col_index].offsetWidth;
			if (max_cell_width[col_index] === undefined) max_cell_width.push(cell_width);
			else if (max_cell_width[col_index] < cell_width) {
				max_cell_width[col_index] = cell_width;
			}
		}
	}
	for (table_index = 1; table_index<rows1.length; table_index++){
		var td = table2.rows[table_index].cells[4];
		td.style.padding = "0px";
		var table = table2.rows[table_index].cells[4].firstChild;
		for (col_index = 0; col_index<table.rows[0].cells.length; col_index++){
			table.rows[0].cells[col_index].width = max_cell_width[col_index];
		}
	}
}

function createTableTeamResults(tableData, nameColIndex) {
	if (tableData == "") return;

    var table = document.createElement('table');
    var tableBody = document.createElement('tbody');

    for (td = 0; td < tableData.length; td++) {
		var row = document.createElement('tr');
		for (cd = 0; cd < tableData[td].length; cd++) {
			var cell = document.createElement('td');
			if (tableData[td][cd].tagName === "TABLE") {
				cell.appendChild(tableData[td][cd]);
			} else {
				cell.appendChild(createTableListItemTeamResults(tableData[td][cd], td, cd, nameColIndex));
			}
			row.appendChild(cell);
		}
		tableBody.appendChild(row);
    }

    table.appendChild(tableBody);
    return table;
}

function createTableListItemTeamResults(cellData, rowIndex, colIndex, nameColIndex){
	var span;
	if (rowIndex == 0) {
        span = createHeaderSpanTeamResults(cellData);
    }
	else if (colIndex == nameColIndex) {
		span = createSpanTeamResults(cellData, 'span-left');
	} else {
		span = createSpanTeamResults(cellData, 'span');
	}
	return span;
}

function createSpanTeamResults(cellData, spanClassName){
	var span = document.createElement('span');
	$(span).addClass(spanClassName);
	$(span).html(cellData);
	return span;
}

function createHeaderSpanTeamResults(cellData){
	var span = document.createElement('span');
    $(span).addClass('span-header');
	$(span).html(cellData);
	return span;
}
