function pendingScreen(data){
	var list = document.createElement('div');
	$(list).addClass('pending list');

	$('#race-name h2').html(data.race_name);
	return list;
}

function render_pending(){
	
	var list = $('div.pending');

	$(list).html("<h3>Race Not Started</h3>");
}