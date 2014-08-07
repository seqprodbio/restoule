package models;

import scala.collection.mutable.Map
import scala.io.Source
import org.json._

object LIMSRetrieval {
   def getSamplesString(): String = {
      var samplesText = Source.fromURL("https://pinery.hpc.oicr.on.ca:8443/pinery/samples")
      return samplesText.mkString
   }

   def getDonor(samples: String, startSample: JSONObject): String = {
      var sample: JSONObject = startSample
      while (sample.has("sample") && sample.getJSONObject("sample").has("parents")) {
         var sampleString: String = "";
         var parentMatchURL: Integer = samples.indexOf("\"url\":\"" + sample.getJSONObject("sample").getString("parents").replace("/", "\\/") + "\"")
         var sampleStart: Integer = samples.lastIndexOf("{\"sample\":{", parentMatchURL + 1)
         var sampleEnd: Integer = samples.indexOf("}}", parentMatchURL + 1)

         if (sampleStart != -1 && sampleEnd != -1 && (sampleStart != 0 || sampleEnd != samples.length() - 1)) {
            sampleString = samples.substring(sampleStart, sampleEnd + 2)
         } else {
            sampleString = "{}"
         }
         sample = new JSONObject(sampleString)
      }
      if (!sample.has("sample")) {
         return ""
      } else {
         return sample.getJSONObject("sample").getString("name")
      }
   }

   def getLibraryInfo(samples: String, startSample: JSONObject): Map[String, String] = {
      var sample: JSONObject = startSample
      while (sample.has("sample")) {
         if (sample.getJSONObject("sample").has("attributes") && sample.getJSONObject("sample").optJSONArray("attributes") != null && isLibrary(sample.getJSONObject("sample").getString("sample_type"))) {
            var returnMap: Map[String, String] = Map()
            var attributes: JSONArray = sample.getJSONObject("sample").optJSONArray("attributes")
            var index = 0
            for (index <- 0 to attributes.length() - 1) {
               if (attributes.optJSONObject(index) != null) {
                  var attributeName: String = attributes.getJSONObject(index).getString("name");
                  if (attributeName.equals("Library Strategy") || attributeName.equals("Library Source") || attributeName.equals("Library Selection Process")) {
                     returnMap.put(attributeName, attributes.getJSONObject(index).getString("value"))
                  }
               }
            }
            if (returnMap.contains("Library Strategy") && returnMap.contains("Library Source") && returnMap.contains("Library Selection Process")) {
               returnMap.put("Library Name", sample.getJSONObject("sample").getString("name"))
               return returnMap
            }
         }
         var sampleString: String = "{}";
         if (sample.getJSONObject("sample").has("parents")) {
            var parentMatchURL: Int = samples.indexOf("\"url\":\"" + sample.getJSONObject("sample").getString("parents").replace("/", "\\/") + "\"");
            var sampleStart: Int = samples.lastIndexOf("{\"sample\":{", parentMatchURL + 1);
            var sampleEnd: Int = samples.indexOf("}}", parentMatchURL + 1);

            if (sampleStart != -1 && sampleEnd != -1 && (sampleStart != 0 || sampleEnd != samples.length() - 1)) {
               sampleString = samples.substring(sampleStart, sampleEnd + 2);
            }
         }
         sample = new JSONObject(sampleString);
      }
      return Map[String, String]()
   }

   def isLibrary(sampleType: String): Boolean = {
      if (sampleType.indexOf("Library") != -1 && sampleType.indexOf("Seq") == -1) {
         return true
      } else {
         return false
      }
   }

   def getStartSample(samples: String, libraryName: String): JSONObject = {
      var lastMatch: Integer = -1
      var sampleString = "{}"

      if (libraryName != "") {
         while (samples.indexOf("\"name\":\"" + libraryName, lastMatch + 1) != -1) {
            lastMatch = samples.indexOf("\"name\":\"" + libraryName, lastMatch + 1)
            var sampleStart: Integer = samples.lastIndexOf("{\"sample\":{", lastMatch + 1);
            var sampleEnd: Integer = samples.indexOf("}}", lastMatch + 1);
            var sampleTypeStart: Integer = samples.indexOf("\"sample_type\":", sampleStart);
            if (isLibrary(samples.substring(sampleTypeStart + 15, samples.indexOf("\",", sampleTypeStart) + 1))) {
               sampleString = samples.substring(sampleStart, sampleEnd + 2);
            }
         }
      }
      return new JSONObject(sampleString)
   }
}