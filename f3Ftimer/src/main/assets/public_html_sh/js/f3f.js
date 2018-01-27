jQuery.event.add(window, "resize", resizeFrame);
jQuery.event.add(window, "orientationchange", resizeFrame);
jQuery.event.add(window, "unload", stopLoading);

var scrollwindow = null;
var timer;
var frame_rate = 30; // seconds
var viewStack = [];
var indexPath = [];
var model = {};
var modelLive = {};
modelLive.state = 0;
model.time = 0;
var xhr;
var lastTime = 0;
var lastView = null;

$(document).bind("keydown keypress", function(e){
	if( (e.keyCode == 37 && e.altKey == true) || e.which == 8 ) { // 8 == backspace
		lastView = viewStack[viewStack.length-1];
		popView();
		e.preventDefault();
	}
	if (e.keyCode == 39 && e.altKey == true) {
	    pushView(lastView);
	    e.preventDefault();
	}
});

$(document).ready(function(){
	$('#top div#race-name').on('click', function(){ popView(); });
	getUpdate();
});

function stopLoading(){
	if(xhr && xhr.readystate != 4){
        xhr.abort();
    }
	clearTimeout(timer);
}

function rearmTimer() {
	timer = setTimeout("getUpdate()", frame_rate * 1000);
}

function getUpdate(){
	stopLoading();

    var requesturl;
    if (typeof curr_view !== 'undefined' && curr_view.className == 'liveinfo list') {
        requesturl = "api/getRaceLiveData.jsp?last_request="+lastTime.toString()
    } else {
        requesturl = "api/getRaceData.jsp?last_request="+lastTime.toString();
    }
	try {
	    xhr = new XMLHttpRequest();
		xhr.open("GET", requesturl);
		xhr.onload = function (response) {
			requestResponseFn(response);
		};
		xhr.onerror = function(xhr, textStatus, errorThrown) {
			xhrOnError(xhr, textStatus, errorThrown);
		};
		xhr.send(null);
	}
	catch(err) {
		rearmTimer();
	}
}

function xhrOnError(xhr, textStatus, errorThrown) {
	rearmTimer();
}

function requestResponseFn(response){
    try {
        data = JSON.parse(xhr.responseText)[0];

		if (typeof model.time !== 'undefined' && typeof modelLive.time !== 'undefined' && modelLive.time <= model.time) {
			lastTime = model.time;
		} else if (typeof modelLive.time !== 'undefined') {
			lastTime = modelLive.time;
		} else if (typeof model.time !== 'undefined') {
			lastTime = model.time;
		}

		if (lastTime == 0){
			modelLive = data;
			firstCall(data);
		} else {
			if (typeof curr_view !== 'undefined' && curr_view.className == 'liveinfo list') {
				modelLive = data;
			} else {
				model = data;
			}
		}
		resizeFrame();
    }
    catch(err) {
        console.log(err);
    	rearmTimer();
    }
}

function firstCall(data){
	pushView(homeScreen());
}

function resizeFrame(){
	render();
}

function render(){
	clearTimeout(timer);
	if (viewStack.length<1) return;
	curr_view = viewStack[viewStack.length-1];
	classes = $(curr_view).attr('class').split(' ');
	cls = classes[0];
	eval("render_"+cls+"()");
	if (scrollwindow != null) {
        scrollwindow.refresh();
    } else {
        scrollwindow = new IScroll(curr_view, { zoom: true, mouseWheel: true, click: false, tap: false});
    }
	if (curr_view.className == 'liveinfo list') {
	    frame_rate = 1;
    } else {
	    frame_rate = 30;
    }
    rearmTimer();
	if (curr_view.className == 'home list') {
	    clearTimeout(timer);
	}
}

function pushView(view, context){
	$('#window').scrollTop(0);

	$('#view-stack').append(view);
	$('#view-stack').stop().animate({'height':1}, 100, "swing", function(){
		if (viewStack.length<2) return;
		oldview = viewStack[viewStack.length-2];
		$(oldview).addClass('out').scrollTop(0);
	});

	viewStack.push(view);

	if (viewStack.length>1){
		if (!$('#top').hasClass('back'))
			$('#top').addClass('back');

		indexPath.push($(context).index());
	}
	getUpdate();
}

function popView(){
	if (viewStack.length == 1) return;
	$('#window').scrollTop(0);
	scrollwindow = null;
	lastView = viewStack.pop();
	indexPath.pop();
	oldview = curr_view = viewStack[viewStack.length-1];
	$(oldview).removeClass('out');

	$('#view-stack').stop().animate({'height':0}, 100 , "swing", function(){
		$(lastView).remove();
	});

	if (viewStack.length==1 && $('#top').hasClass('back')) $('#top').removeClass('back');
	resizeFrame();
}

function responsive_name(str){
	var w = $(window).width();
	if (w<480){
		var parts = str.split(' ');
		str = parts[0].substring(0,1)+' '+parts[1];
	}
	return str;
}

/* Maths Functions */
function round2Fixed(value, places) {
    value = +value;

	if (isNaN(value))
	    return NaN;

    var multiplier = Math.pow(10, places);
	return (Math.round(value * multiplier) / multiplier);
}

function clearList(list){
	$(list).html('');
}

function createListItem(label, callback){
	var li = document.createElement('div');
	$(li).addClass('list-item');
	$(li).html('<span>'+label+'</span>');
	if (callback) $(li).bind('click', callback);
	return li;
}

function createListItemLink(label, callback){
	var li = document.createElement('div');
	$(li).addClass('list-item-link');
	$(li).html('<span>'+label+'</span>');
	if (callback) $(li).bind('click', callback);
	return li;
}

function createSpan(cellData, spanClassName){
	var span = document.createElement('span');
	span.classList.add(spanClassName);
	span.innerHTML = cellData;
	return span;
}
