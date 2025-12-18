/* utils.js 1.0 Oct 2018-2023 by jivecast.com */
// "use strict";

// ---------------------------------------------------------------------------------------------------
function setEmptyOption(elementId) {
	var blank = new Option("N/A", 0, false, false);
	elementId.options.length = 0;
	elementId[0] = blank;
	return;
}
// ---------------------------------------------------------------------------------------------------
function reportWindow(windowURL) {
	// var w = screen.availWidth * 0.65;
	// var h = screen.availHeight * 0.65;
	// var options = "width=" + w + ",height=" + h +
	// ",toolbar=1,location=0,directories=0,status=0,menuBar=0,scrollBars=1,resizable=1,top=15,left=15";
	window.open(windowURL, '_blank');
	return;
}
// ---------------------------------------------------------------------------------------------------
function preset() {

	// Homepage
	if (document.login)
		document.login.username.focus();
	
	// Password page
	if (document.passwordUpdateForm)
		document.passwordUpdateForm.newPassword.focus();
}


//---------------------------------------------------------------------------------------------------
//section hiding
//---------------------------------------------------------------------------------------------------
function openTab(evt, tabName) {
 var i, x, tablinks;
 var myColor = " w3-pink";
 x = document.getElementsByClassName("tab");
 for (i = 0; i < x.length; i++) {
 	// hide all the panels
 	x[i].className = x[i].className.replace(" w3-show", "");
 }
 tablinks = document.getElementsByClassName("tablink");
 for (i = 0; i < x.length; i++) {
 	if ( tablinks[i] != null )
 		tablinks[i].className = tablinks[i].className.replace(myColor, "");
 }
 
 document.getElementById(tabName).className += " w3-show";
 
 if ( evt )
 	evt.currentTarget.className += myColor;
	return;
}

function openPanel(panel) {
	var x = document.getElementsByClassName("panel");
 for (i = 0; i < x.length; i++) {
 	x[i].className = x[i].className.replace(" w3-show", "");
 }
	
 var y = document.getElementById(panel);
 if (y.className.indexOf("w3-show") == -1) {
     y.className += " w3-show";
 } else { 
     y.className = y.className.replace(" w3-show", "");
 }
}
