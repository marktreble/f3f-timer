jQuery.event.add(window, "load", resizeFrame);
jQuery.event.add(window, "resize", resizeFrame);
jQuery.event.add(window, "orientationchange", resizeFrame);
jQuery.event.add(window, "unload", stopLoading);

var scrollwindow;
var timer;
var frame_rate = 60; // seconds
var viewStack = [];
var indexPath = [];
var model = {};
model.time = 0;
var xhr;

$(document).ready(function(){
	$('#top div#race-name').on('click', function(){ popView(); });

	$(document).bind('touchmove', function(e){
		e.preventDefault();
	})
	scrollwindow = new IScroll('#window', { mouseWheel: true, click: true, resize: true });

	getUpdate();

});

function stopLoading(){
	if(xhr && xhr.readystate != 4){
        xhr.abort();
    }
	clearTimeout(timer);
}

function resizeFrame(){
	render();
	h = $(window).height();
	header_height = $('#top').height();
	$('#window').height(h - header_height);
	scrollwindow.refresh();
}
function getUpdate(){
	stopLoading();
	xhr = $.ajax({
		url:"api/getRaceData.jsp?last_request="+model.time.toString()+"&nocache="+(new Date()).getTime(),
		dataType:"json",
		success: function(response){
			data = eval(response);
			if (model.time == 0){
				firstCall(data[0]);
			} else {
				updateCall(data[0]);
			}
		},
		error: function(){
			// Just call render to continue the timeout loop
			render();
		}
	});

}

function firstCall(data){
	model = data;
	if (data.race_status > 0){
		pushView(homeScreen());
	} else {
		pushView(pendingScreen(data))
	}
	resizeFrame();
}

function updateCall(data){
	if (model.race_status == 0 && data.race_status > 0){
		lastView = viewStack.pop();
		$(lastView).remove();
		pushView(homeScreen());
	}
	model = data;
	resizeFrame();
}

function render(){
	clearTimeout(timer);
	if (viewStack.length<1) return;
	curr_view = viewStack[viewStack.length-1];
	classes = $(curr_view).attr('class').split(' ');
	cls = classes[0];
	eval("render_"+cls+"()");
	timer = setTimeout("getUpdate()", frame_rate * 1000);
}

function pushView(view, context){
	$('#window').scrollTop(0);
	scrollwindow.scrollTo(0,0);

	$('#view-stack').append(view);
	$('#view-stack').stop().animate({'margin-left':(-viewStack.length*100).toString()+'%'}, 300, "swing", function(){
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
	resizeFrame();
}

function popView(){
	if (viewStack.length == 1) return;
	$('#window').scrollTop(0);
	scrollwindow.scrollTo(0,0);
	lastView = viewStack.pop();
	indexPath.pop();
	oldview = curr_view = viewStack[viewStack.length-1];
	$(oldview).removeClass('out');

	$('#view-stack').stop().animate({'margin-left':((1-viewStack.length)*100).toString()+'%'}, 300 , "swing", function(){
		$(lastView).remove();
	});

	if (viewStack.length==1 && $('#top').hasClass('back')) $('#top').removeClass('back');
	resizeFrame();
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

	  multiplier = Math.pow(10,places);
	  integer = Math.floor(value);
	  precision = Math.floor((value-integer) * multiplier)

	  return fixDec(integer + (precision/multiplier));
}

function fixDec(n){
    int = Math.floor(n);
    dec = Math.floor(n*100)-(int*100);
    dec = dec.toString();
    while (dec.length<2) dec='0'+dec;
    dec ="."+dec;
    int = int.toString();

    return int+dec;
}