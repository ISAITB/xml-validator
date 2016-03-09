function fileInputChanged() {
	$('#inputFileName').val($('#inputFile')[0].files[0].name);
	$('#inputFileSubmit').prop('disabled', false);
}
function triggerFileUpload() {
	$('#inputFile').click();
}
function uploadFile() {
	waitingDialog.show('Validating input', {dialogSize: 'm'});
	return true;
}
