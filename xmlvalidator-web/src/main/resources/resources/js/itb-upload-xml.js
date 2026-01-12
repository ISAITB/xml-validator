/*
 * Copyright (C) 2026 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

addListener('VALIDATION_TYPE_CHANGED', updateContextFiles);
addListener('SUBMIT_STATUS_VALIDATED', validateSubmitStatus);
addListener('BEFORE_SUBMIT', prepareContextFileCodeEditors);

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
                        "<option value='stringType'>"+_config.externalArtifactTextLabel+"</option>"+
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
                        "<div class='col-sm-12 hidden' id='stringContainer-"+elementId+"'>"+
                            "<textarea id='text-editor-"+elementId+"' class='form-control inputChangedContextFile'></textarea>"+
                            "<input type='hidden' id='text-editor-value-"+elementId+"' name='text-contextFile' data-element-id='"+elementId+"'>"+
                        "</div>"+
                        "<input type='file' class='inputFile inputChangedContextFile' id='inputFile-"+elementId+"' name='inputFile-contextFile' data-element-id='"+elementId+"'/>" +
                    "</div>"+
                "</div>"+
            "</div>");
        "</div>" +
    "</div>"
    $(".context-files").append(elements);
    // Activate code editor.
    if (document.getElementById("text-editor-"+elementId) !== null){
        CodeMirror.fromTextArea(document.getElementById("text-editor-"+elementId), {
            mode: _config.codeTypeObj,
            lineNumbers: true
        }).on('change', function(){
            inputChangedContextFile(elementId);
        });
    }
}

function contentTypeChangedContextFile(elementId) {
	var type = $('#contentType-'+elementId).val();
	if (type == "uriType"){
		$("#uriContainer-"+elementId).removeClass('hidden');
		$("#fileContainer-"+elementId).addClass('hidden');
		$("#stringContainer-"+elementId).addClass('hidden');
	} else if (type == "fileType"){
		$("#fileContainer-"+elementId).removeClass('hidden');
		$("#uriContainer-"+elementId).addClass('hidden');
		$("#stringContainer-"+elementId).addClass('hidden');
	} else if (type == "stringType") {
		$("#stringContainer-"+elementId).removeClass('hidden');
		$("#fileContainer-"+elementId).addClass('hidden');
		$("#uriContainer-"+elementId).addClass('hidden');
		setTimeout(function() {
            var codeMirror = getCodeMirrorNative('#text-editor-'+elementId)
            codeMirror.refresh();
            updateSubmitStatus();
		}, 0);
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

function getContextFileCount() {
    var validationType = getCompleteValidationType();
    var contextFileCount = 0;
    if (contextFiles[validationType]) {
        contextFileCount = contextFiles[validationType].length
    }
    return contextFileCount;
}

function validateSubmitStatus() {
    setTimeout(function() {
        var contextFileCount = getContextFileCount();
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
                    var contentType = $("#contentType-"+elementId).val();
                    if ((contentType == "uriType" && !$("#uri-"+elementId).val()) ||
                        (contentType == "fileType" && !$("#inputFileName-"+elementId).val()) ||
                        (contentType == "stringType" && !getCodeMirrorNative('#text-editor-'+elementId).getDoc().getValue())) {
                        submitButton.prop('disabled', true);
                        break;
                    }
                }
            }
        }
    }, 1)
}

function prepareContextFileCodeEditors() {
    var contextFileCount = getContextFileCount();
    for (var i=0; i < contextFileCount; i++) {
        var elementId = toContextFileElementId(i);
        var contentType = $("#contentType-"+elementId).val();
        if (contentType == "stringType") {
            $("#text-editor-value-"+elementId).val(getCodeMirrorNative('#text-editor-'+elementId).getDoc().getValue());
        } else {
            $("#text-editor-value-"+elementId).val('');
        }
    }
}