function validationTypeChanged() {
	checkForSubmit();
}
function checkForSubmit() {
	var type = $('#contentType').val();
	var inputType = $('#validationType');
	$('#inputFileSubmit').prop('disabled', true);
	
	if(type == "fileType"){
		var inputFile = $("#inputFileName");
		$('#inputFileSubmit').prop('disabled', (inputFile.val() && (!inputType.length || inputType.val()))?false:true);
	}
	if(type == "uriType"){
		var uriInput = $("#uri");
		$('#inputFileSubmit').prop('disabled', (uriInput.val() && (!inputType.length || inputType.val()))?false:true);		
	}
	if(type == "stringType"){
		var stringType = getCodeMirrorNative('#text-editor').getDoc();	
		var contentType = $("#contentSyntaxType");
		
		$('#inputFileSubmit').prop('disabled', (stringType.getValue() && (!contentType.length || contentType.val()) && (!inputType.length || inputType.val()))?false:true);		
	}
}
function triggerFileUpload() {
	$('#inputFile').click();
}
function uploadFile() {
	waitingDialog.show('Validating input', {dialogSize: 'm'}, isMinimal?'busy-modal-minimal':'busy-modal');
	return true;
}

function contentTypeChanged(){
	var type = $('#contentType').val();
	$('#inputFileSubmit').prop('disabled', true);
	
	if(type == "uriType"){
		$("#uriToValidate").removeClass('hidden');
		$("#fileToValidate").addClass('hidden');
		$("#stringToValidate").addClass('hidden');
	} else if(type == "fileType"){
		$("#fileToValidate").removeClass('hidden');
		$("#uriToValidate").addClass('hidden');
		$("#stringToValidate").addClass('hidden');
	} else if(type == "stringType"){
		$("#stringToValidate").removeClass('hidden');
		$("#uriToValidate").addClass('hidden');
		$("#fileToValidate").addClass('hidden');
		setTimeout(function() {
            var codeMirror = getCodeMirrorNative('#text-editor')
            codeMirror.refresh();
		}, 0);
	}
}
function getCodeMirrorNative(target) {
    var _target = target;
    if (typeof _target === 'string') {
        _target = document.querySelector(_target);
    }
    if (_target === null || !_target.tagName === undefined) {
        throw new Error('Element does not reference a CodeMirror instance.');
    }
    
    if (_target.className.indexOf('CodeMirror') > -1) {
        return _target.CodeMirror;
    }

    if (_target.tagName === 'TEXTAREA') {
        return _target.nextSibling.CodeMirror;
    }
    
    return null;
}
function contentSyntaxChanged() {
	checkForSubmit();
}
function fileInputChanged() {
	if($('#contentType').val()=="fileType"){
		$('#inputFileName').val($('#inputFile')[0].files[0].name);
	}
	checkForSubmit();
}
$(document).ready(function() {
	validationTypeChanged();

	if(document.getElementById('text-editor') !== null){
		var editableCodeMirror = CodeMirror.fromTextArea(document.getElementById('text-editor'), {
	        mode: "xml",
	        lineNumbers: true
	    }).on('change', function(){
	    	contentSyntaxChanged();
	    });
	}
});