function homeScreen(){
	var list = document.createElement('div');
	$(list).addClass('home list');
	return list;
}

function render_home(){
	$('#race-name h2').html(model.race_name + " &ndash; Round "+model.current_round);

	var list = $('div.home');

	clearList(list);

	var lis = [];

	var li =createListItem("Round in Progress (R"+model.current_round+")", function(){
		pushView(roundInProgress(), this);
	});
	lis.push(li);

	li = createListItem("Completed Rounds", function(){
		pushView(completedRounds(), this);
	});
	lis.push(li);

	li = createListItem("Leader Board", function(){
		pushView(leaderBoard(), this);
	});
	lis.push(li);

	for(var i in lis){
		$(list).append(lis[i]);
	}
}