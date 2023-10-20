addListener('VALIDATION_TYPE_CHANGED', updateContextFiles);
addListener('SUBMIT_STATUS_VALIDATED', validateSubmitStatus);

function updateContextFiles(eventType, eventData) {
    clearContextFiles()
    if (eventData.validationType) {
        if (contextFiles[eventData.validationType] && contextFiles[eventData.validationType].length > 0) {
            for (var i=0; i < contextFiles[eventData.validationType].length; i++) {
                addContextFile(i, contextFiles[eventData.validationType][i].label, contextFiles[eventData.validationType][i].placeholder);
            }
            addContextFileEventHandlers();
        }
    }
}

function addContextFileEventHandlers() {
    $(".contentTypeChangedContextFile").off().on("change", function() { contentTypeChangedContextFile($(this).attr("data-element-id")); });
    $(".triggerFileUploadContextFile").off().on("click", function() { triggerFileUploadContextFile($(this).attr("data-file-type")); });
    $(".inputChangedContextFile").off().on("change", function() { inputChangedContextFile($(this).attr("data-element-id")); });
}

function toContextFileElementId(index) {
    return "contextFile_"+index;
}

function addContextFile(index, labelText, placeholderText) {
    var elementId = toContextFileElementId(index);
    var elements = $(
    "<div class='form-group'>" +
        "<label for='file-"+elementId+"' class='col-sm-2 control-label'>"+labelText+"</label>" +
        "<div class='col-sm-10'>" +
            "<div class='row' id='"+elementId+"'>" +
                "<div class='col-sm-2'>"+
                    "<select class='form-control contentTypeChangedContextFile' id='contentType-"+elementId+"' data-element-id='"+elementId+"' name='contentType-contextFile'>"+
                        "<option value='fileType' selected='true'>"+_config.externalArtifactFileLabel+"</option>"+
                        "<option value='uriType'>"+_config.externalArtifactURILabel+"</option>"+
                    "</select>"+
                "</div>"+
                "<div class='col-sm-10'>" +
                    "<div class='row'>" +
                        "<div id='fileContainer-"+elementId+"' class='col-sm-12'>" +
                            "<div class='input-group' id='file-"+elementId+"'>" +
                                "<div class='input-group-btn'>" +
                                    "<button class='btn btn-default triggerFileUploadContextFile' type='button' data-file-type='inputFile-"+elementId+"'><i class='far fa-folder-open'></i></button>" +
                                "</div>" +
                                "<input type='text' id='inputFileName-"+elementId+"' placeholder='"+placeholderText+"' data-file-type='inputFile-"+elementId+"' class='form-control clickable triggerFileUploadContextFile' readonly='readonly'/>" +
                            "</div>" +
                        "</div>" +
                        "<div class='col-sm-12 hidden' id='uriContainer-"+elementId+"'>"+
                            "<input type='text' class='form-control inputChangedContextFile' id='uri-"+elementId+"' name='uri-contextFile' data-element-id='"+elementId+"'>"+
                        "</div>"+
                        "<input type='file' class='inputFile inputChangedContextFile' id='inputFile-"+elementId+"' name='inputFile-contextFile' data-element-id='"+elementId+"'/>" +
                    "</div>"+
                "</div>"+
            "</div>");
        "</div>" +
    "</div>"
    $(".context-files").append(elements);
}

function contentTypeChangedContextFile(elementId) {
	var type = $('#contentType-'+elementId).val();
	if (type == "uriType"){
		$("#uriContainer-"+elementId).removeClass('hidden');
		$("#fileContainer-"+elementId).addClass('hidden');
	} else if (type == "fileType"){
		$("#fileContainer-"+elementId).removeClass('hidden');
		$("#uriContainer-"+elementId).addClass('hidden');
	}
	checkForSubmit();
}

function triggerFileUploadContextFile(elementId) {
    $("#"+elementId).click();
}

function inputChangedContextFile(elementId) {
	if ($('#contentType-'+elementId).val() == "fileType" && $("#inputFile-"+elementId)[0].files[0] != null) {
		$("#inputFileName-"+elementId).val($("#inputFile-"+elementId)[0].files[0].name);
	}
	checkForSubmit();
}

function clearContextFiles() {
    $(".context-files").empty();
	checkForSubmit();
}

function validateSubmitStatus() {
    setTimeout(function() {
        var validationType = getCompleteValidationType();
        var contextFileCount = 0;
        if (contextFiles[validationType]) {
            contextFileCount = contextFiles[validationType].length
        }
        if (contextFileCount > 0) {
            var submitButton;
            if (_config.isMinimalUI) {
                submitButton = $('#inputFileSubmitMinimal');
            } else {
                submitButton = $('#inputFileSubmit');
            }
            if (!submitButton.is(":disabled")) {
                for (var i=0; i < contextFileCount; i++) {
                    var elementId = toContextFileElementId(i)
                    var contentType = $("#contentType-"+elementId).val();;
                    if ((contentType == "uriType" && !$("#uri-"+elementId).val()) ||
                        (contentType == "fileType" && !$("#inputFileName-"+elementId).val())) {
                        submitButton.prop('disabled', true);
                        break;
                    }
                }
            }
        }
    }, 1)
}
