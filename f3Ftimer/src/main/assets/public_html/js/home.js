function homeScreen(){
	var list = document.createElement('div');
	$(list).addClass('home list');
	return list;
}

function render_home(){
	$('#race-name h2').html(model.race_name);
	
	var list = $('div.home');
	
	clearList(list);
	
	var lis = [];

	li = createListItemLink("Startlist", function(){
		pushView(startlist(), this);
	});
	lis.push(li);

	var li = createListItemLink("Current Flight Live Status", function(){
		pushView(liveInfo(), this);
	});
	lis.push(li);

	li = createListItemLink("Round in Progress (R"+model.current_round+")", function(){
		pushView(roundInProgress(), this);
	});
	lis.push(li);

	li = createListItemLink("Completed Rounds", function(){
		pushView(completedRounds(), this);
	});
	lis.push(li);

	li = createListItemLink("Overall Standings", function(){
		pushView(scores(), this);
	});
	lis.push(li);

	li = createListItemLink("Overall Team Standings", function(){
		pushView(teamresults(), this);
	});
	lis.push(li);

	for(var i in lis){
		$(list).append(lis[i]);
	}
}