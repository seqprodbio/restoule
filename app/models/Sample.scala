package models;

class Sample(sampleName: String, sampleFs: String, sampleBox: String, sampleRun: String, sampleBarcode: String, sampleLibrarySource: String, sampleLibraryStrategy: String, sampleComplete: Boolean) {
   var name: String = sampleName;
   var Fs: String = sampleFs;
   var box: String = sampleBox;
   var run: String = sampleRun;
   var barcode: String = sampleBarcode;
   var librarySource: String = sampleLibrarySource;
   var libraryStrategy: String = sampleLibraryStrategy;
   var complete = sampleComplete;
}