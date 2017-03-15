

var fileTree, map, mapMarkers;
var filter = {};
var markersCount = 0;
var currentPath = "0";
var complete = true;


var progress = document.getElementById('progress');
var progressBar = document.getElementById('progress-bar');
function updateProgressBar(processed, total, elapsed, layersArray) {
	if (elapsed > 500) {
		progress.style.display = 'block';
		progressBar.style.width = Math.round(processed/total*100) + '%';
	}
	if (processed === total) {
		progress.style.display = 'none';
		complete = true;
	}
}


$(document).ready(function() {
	fitViewport();
	
	map = L.map('map', {worldCopyJump: true}).setView([43.905, -85.7125], 10); // nirvana
	L.tileLayer('https://api.mapbox.com/styles/v1/mapbox/outdoors-v10/tiles/256/{z}/{x}/{y}?access_token=pk.eyJ1IjoiaGFzaG1pY2giLCJhIjoiY2lubDhraDE0MDA4OHc5bTIybDRia3NuMSJ9.zGyzM6Se8Yh4R-SC8sxmUQ', {
	    attribution: 'Map data &copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors, <a href="http://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>, Imagery © <a href="http://mapbox.com">Mapbox</a>',
	    maxZoom: 18
	}).addTo(map);
	
	// the show in filetree function
	map.on('popupopen', function() {
		$('#map .leaflet-popup-pane .leaflet-popup-content-wrapper span.gotoTree').click(function() {
			var path = $(this).attr('data-pathId');
			var listelement = $('#directories li[data-pathId="'+path+'"] > span.item');
			listelement.click();
			// scroll listelement to top
			$("#leftcol").animate({ scrollTop: listelement.offset().top - 100 }); 
		});
	});
	
	mapMarkers = new L.MarkerClusterGroup({
		spiderfyOnMaxZoom: true,
		//disableClusteringAtZoom: 14,
		showCoverageOnHover: true,
		zoomToBoundsOnClick: true,
		maxClusterRadius: 30,
		chunkedLoading: true,
		chunkProgress: updateProgressBar
	});
	
	$.when(getTree()).then(function(data) {
		fileTree = data;
		$.when(drawFiletree()).then(function() {
			addFileTreeHandlers();
		});
	});
	
	readFilterForm();
	$('#filters input').on('change', function() {
		readFilterForm();
		drawMapIcons(currentPath);
	});
});

$(window).resize(function() {
	fitViewport();
});



function readFilterForm() {
	$('#filters input[type=checkbox]').each(function() {
		filter[this.name] = $(this).is(":checked");
	});
	$('#filters input[type=radio]:checked').each(function() {
		filter[this.name] = $(this).val();
	});
}

function addFileTreeHandlers() {
	// we can only provide one handler for both closing & opening, 
    // as we cannot do a selection for future change of classnames
	$('#directories li > span.item').click(function(e) {
		$('#directories span.item').removeClass('active');
	});
	
	$('#directories li.directory span.item.directory').click(function(e) {
    	var directory = $(this).parent();
    	if(directory.hasClass('directory')) {
            e.stopPropagation();
            
            // open a directory
            if (directory.hasClass('closed')) {
                filebrowserOpenFolder(directory);
                $(this).addClass('active');
            }
            // close a directory
            else if (directory.hasClass('open')) {
                filebrowserCloseFolder(directory);
                selectParent(directory);
            }
        }
    });
	
	$('#directories .file.item').click(function(e) {
		// hide all others
		$('#directories li .well').not($(this).siblings('.well')).hide();
		$(this).siblings('.well').show();
		$(this).addClass('active');
		filebrowserOpenAnchestors($(this));
		var fileItem = $(this).parent();
		var path = fileItem.attr('data-pathId');
		currentPath = path;
		drawMapIcons(path);
	});
	
	// open the current folder or item
    $('[data-pathId="' + currentPath + '"] > span.item').click();
}

function fitViewport() {
	var viewportHeight = document.body.clientHeight;
	var viewportWidth = document.body.clientWidth;
	var margins = $('#header').outerHeight() + $('#footer').outerHeight() + 15;
	$('#columns .col').css('height', viewportHeight - margins);
	$('#columns .col.left').css('height', viewportHeight - margins - 50);
	$('#map').css('height', viewportHeight - margins);
}

function getTree() {
	return $.getJSON("json/filetree.json");
}

function drawFiletree() {
	var html = [];
	html.push('<ul>');
	html.push(drawSubtree().join(''));
	html.push('</ul>');
	$('#directories').html(html.join(''));
}

function drawSubtree(path, tree) {
	if(typeof tree == 'undefined') tree = fileTree;
	var html = [];
	if(typeof path == 'undefined') {
		path = '0';
	}
	
	html.push('<li data-pathId="'+path+'" class="directory closed">');
	html.push('<span class="item directory">');
	html.push('<span class="glyphicon glyphicon-folder-close"></span>&nbsp;&nbsp;'+tree['name']+'</span>');
	
	html.push('<div class="well" style="display:none;">');
	html.push(tree['readable_size']+' in '+tree['directories_count']+'('+tree['cumulated_directory_count']+') folders, '+tree['files_count']+'('+tree['cumulated_file_count']+') files');
	html.push('</div>');
	
	if(tree['directories'].length > 0 || tree['files'].length > 0) {
		html.push('<ul style="display:none">');
		if(tree['directories'].length > 0) {
			for(var i = 0, len = tree['directories'].length; i < len; i++) {
				var subtree = getSubTree(path+'.directories.'+i);
				html.push(drawSubtree(path+'.directories.'+i, subtree).join(''));
			}
		}
		if(tree['files'].length > 0) {
			for(var i = 0, len = tree['files'].length; i < len; i++) {
				var file = tree['files'][i];
				html.push('<li data-pathId="'+path+'.files.'+i+'" class="file">');
				html.push('<span class="item file">');
				html.push('<span class="glyphicon glyphicon-file"></span> '+file['name']);
				html.push('</span>');
				html.push('<div class="well" style="display:none;">');
				html.push(file['readable_size'] + ' | ');
				html.push(getObjectLinks(file, ' | ', null));
				var image = getPreview(null, file);
				if(image != null) html.push('<br>' + image);
				html.push('</div>');
				html.push('</li>');
			}
		}
		html.push('</ul>');
	}
	html.push('</li>');
	
	return html;
}

function getSubTree(path) {
	var tree = fileTree;
	if(!$.isArray(tree)) tree = [tree];
	var split = path.split('.');
	for(var i = 0, len = split.length; i < len; i++) 
		tree = tree[split[i]];
	
	return tree;
}

function filebrowserCloseFolder(selected) {
    selected.toggleClass('closed open');
	selected.find('span.glyphicon-folder-open').toggleClass('glyphicon-folder-open glyphicon-folder-close');
    selected.find('> ul').hide();
	selected.find('> div.well').hide();
    var descendants = selected.find('li.directory.open');
    if(typeof(descendants) && descendants.length !== 0)
        filebrowserCloseFolder(descendants);
}

function selectParent(directory) {
	// show the parent directory's contents
    // remove the last elements from the path - as we want to select the parent dir
    var patharray = directory.attr('data-pathId').split('.');
   // do only move further up if not already at the root element:
	if(patharray.length > 2) {
		patharray.pop();
		patharray.pop();
	}
    var path = patharray.join('.');
    currentPath = path;
    var parent = $('[data-pathId="' + currentPath + '"]');
    parent.find('> span.item').addClass('active');
    $('#directories .well').hide();
	parent.find('> div.well').show();
    drawMapIcons(path);
}

function filebrowserOpenFolder(selected) {
	// close all sibling directories recursively
    filebrowserCloseFolder(selected.siblings('li.directory.open'));
    filebrowserOpenAnchestors(selected);
    // show the selected dir content
    selected.toggleClass("closed open").find(" > ul").slideToggle('fast');
	selected.find('> span.item.directory span.glyphicon-folder-close').toggleClass('glyphicon-folder-close glyphicon-folder-open');
	// show the belonging well, hide all others
	$('#directories .well').hide();
	selected.find('> div.well').show();
	var path = selected.attr('data-pathId');
	currentPath = path;
	drawMapIcons(path);
}

function filebrowserOpenAnchestors(selected) {
	// assert all parent dirs are open
    selected.parentsUntil("#directories", "li.directory.closed").each(function(i, dir) {
    	$(dir).toggleClass('closed open');
    	$(dir).find('span.glyphicon-folder-close').toggleClass('glyphicon-folder-close glyphicon-folder-open');
    	$(dir).find('> ul').show();
    });
}



function drawMapIcons(path) {
	markersCount = 0;
	complete = false;
	// clear the map
	if(typeof(mapMarkers) != 'undefined')
		mapMarkers.clearLayers();
	var markers = recursiveGetCollectionObjectMarkers(path);
	if(markers.length > 0) {
		mapMarkers.addLayers(markers);
		mapMarkers.addTo(map);
		checkComplete();
		map.setMaxZoom(18);
	}else{
		// nirvana
		map.setView([43.905, -85.7125], 14);
	}
}

function checkComplete() {
    if(complete == false) {
    	window.setTimeout(checkComplete, 100); /* this checks the flag every 100 milliseconds*/
    }else{
    	map.fitBounds(mapMarkers.getBounds(), {maxZoom : 10, padding: [20, 20]});
    }
}


function recursiveGetCollectionObjectMarkers(path) {
	var tree = getSubTree(path);
	var markers = [];
	
	var collectionObjectMarkers = getCollectionObjectMarkers(tree);
		if(collectionObjectMarkers.length > 0)
			for(var i = 0; collectionObjectMarkers.length > i; i++)
				markers.push(collectionObjectMarkers[i]);
	
	if(filter.DEPTH == "depth-item") return markers;
	
	if(tree.directories.length > 0) {
		for(var i = 0; tree.directories.length > i; i++) {
			collectionObjectMarkers = recursiveGetCollectionObjectMarkers(path+'.directories.'+i);
			if(collectionObjectMarkers.length > 0)
				for(var n = 0; collectionObjectMarkers.length > n; n++)
					markers.push(collectionObjectMarkers[n]);
		}
	}
	if(tree.files.length > 0) {
		for(var i = 0; tree.files.length > i; i++) {
			var file = tree.files[i];
			collectionObjectMarkers = getCollectionObjectMarkers(file);
			if(collectionObjectMarkers.length > 0)
				for(var n = 0; collectionObjectMarkers.length > n; n++)
					markers.push(collectionObjectMarkers[n]);
		}
	}
	
	return markers;
}

function getCollectionObjectMarkers(collectionObject) {
	var markers = [];
	var geoRefObject;
	
	if(filter.TYPE != 'type-both') {
		if(filter.TYPE == 'type-files' && !collectionObject.is_file) return markers;
		if(filter.TYPE == 'type-folders' && !collectionObject.is_dir) return markers;
	}
	
	if(filter.FILENAME_MENTION) {
		var results = collectionObject.name_results;
		if(results != null && results.length > 0)
			for(var i = 0, len = results.length; i < len; i++)
				markers.push(geoRefMarker(results[i], collectionObject));
	}
	
	if(filter.METADATA_MENTION) {
		results = collectionObject.metadata_place_result;
		if(results != null) markers.push(geoRefMarker(results, collectionObject));
	}
	
	if(filter.METADATA_COORDINATE) {
		results = collectionObject.metadata_coordinate_result;
		if(results != null) markers.push(geoRefMarker(results, collectionObject));
	}
	
	if(filter.CONTENT_COORDINATE) {
		results = collectionObject.content_coordinate_result;
		if(results != null && results.length > 0)
			for(var i = 0, len = results.length; i < len; i++)
				markers.push(geoRefMarker(results[i], collectionObject));
	}
	
	if(filter.CONTENT_MENTION) {
		results = collectionObject.content_place_result;
		if(results != null && results.length > 0)
			for(var i = 0, len = results.length; i < len; i++)
				markers.push(geoRefMarker(results[i], collectionObject));
	}
	
	return markers;
}

function geoRefMarker(geoRefObject, collectionObject) {
	markersCount++;
	var marker = L.marker([geoRefObject.lat,geoRefObject.lon]);
	var html = [];
	var name = geoRefObject.name;
	if(name == null) name = geoRefObject.lon + " " + geoRefObject.lat;
	html.push('<strong>' + name + '</strong>');
	var findlevel;
	switch(geoRefObject.geoRefSource.toLowerCase()) {
	case 'filename_mention':
		findlevel = 'Found <strong>' + geoRefObject.query + '</strong> in ';
		if(collectionObject.is_file) findlevel += 'filename';
		else findlevel += 'foldername';
		break;
	case 'metadata_mention':
		findlevel = 'Found <strong>' + geoRefObject.query + '</strong> in file metadata';
		break;
	case 'metadata_coordinate':
		findlevel = 'Found coordinate in file metadata';
		break;
	case 'content_mention':
		findlevel = 'Found <strong>' + geoRefObject.query + '</strong> in content body';
		break;
	case 'content_coordinate':
		findlevel = 'Found coordinate in content body';
		break;
	}
	html.push(findlevel);
	html.push(((collectionObject.is_file) ? 'Filename: ' : 'Foldername: ') + collectionObject.name);
	var line = "";
	if(geoRefObject.line != null) line = ', line ' + geoRefObject.line;
	html.push('Detector: ' + geoRefObject.detector + line);
	var links = getObjectLinks(collectionObject, '<br>', geoRefObject);
	if(links != "") html.push(links);
	html.push('<span class="action gotoTree" data-pathId="' + collectionObject.pathId + '">Show in file tree</span>');
	if(collectionObject.is_file) {
		var image = getPreview(geoRefObject, collectionObject);
		if(image != null) html.push(image);
	}
	marker.bindPopup(html.join('<br>'));
	return marker;
}

function getObjectLinks(collectionObject, separator, geoRefObject) {
	var html = [];
	if(typeof(separator) == 'undefined' || separator == null) separator = '<br>';
	if(collectionObject.is_file) {
		html.push('<a class="action" href="' + collectionObject.webpath + '" download>Download file</a>');
		// make a preview link for PDF & image files
		var image = ['jpg','jpeg','gif','png','tiff'];
		var ext = collectionObject.tika_mediatype.split('/')[1];
		if(typeof(ext) != 'undefined' && (image.indexOf(ext) >= 0 || ext == 'pdf')) {
			var page = '';
			//if(ext == 'pdf' && geoRefObject...page != null) page = '#page=' + geoRefObject...
			html.push('<a class="action" href="' + collectionObject.webpath + page + '" target="_blank">Preview file (new tab)</a>');
		}	
	}
	return html.join(separator);
}

function getPreview(geoRefObject, collectionObject) {
	var out = null;
	var image = ['jpg','jpeg','gif','png','tiff'];
	var ext = collectionObject.tika_mediatype.split('/')[1];
	if(typeof(ext) != 'undefined' && image.indexOf(ext) >= 0) {
		out = '<img class="img-responsive" src="' + collectionObject.webpath + '" alt="' + collectionObject.name + '">';
	}else if(geoRefObject != null){
		if(typeof geoRefObject.excerpt != 'undefined') out = "<strong>Excerpt:</strong> ..." + geoRefObject.excerpt + "...";
	}
	return out;
}












