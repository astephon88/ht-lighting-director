/**
 *  Home Theater Lighting Director 
 *
 *  Copyright 2017 Andrew Stephon
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
definition(
    name: "Home Theater Lighting Director ",
    namespace: "astephon88",
    author: "Andrew Stephon",
    description: "Set lighting levels based on the playback status of a media player",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/smartlights.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/smartlights@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/smartlights@3x.png")


preferences {
	page (name: "examplePage")
}

def examplePage(){
	dynamicPage(name: "examplePage",title:"",install:true,uninstall:true){
		section("Kodi Settings") {
			input "kodi","device.kodiMediaCenter", required:true, multiple: false, title: "Media Player"
		
    		input "mediaTypes","enum",required:true,multiple:true,title:"Media Type",options: ["TV Show","Movie"]
    	}
    	section("Light Settings"){
    		input "light","capability.light",required:true,multiple:false,title:"Light",submitOnChange:true
    	
    		if (light){
            	input "playingState", "bool",required:true, title:"On When Playing", defaultValue:false,submitOnChange:true
                if (light.hasCapability("Switch Level")&&playingState){
                	input "playingLevel","number",required:true,title:"Play Level",range:"0..100",defaultValue:25
                }
                input "pausedState", "bool", required:true, title:"On When Paused", defaultValue:false,submitOnChange:true
                if (light.hasCapability("Switch Level")&&pausedState){
                	input "pausedLevel","number",required:true,title:"Pause Level",range:"0..100",defaultValue:50
                }
    				
			}
        }
        section([mobileOnly:true]) {
            label title: "Assign a name", required: false
            mode title: "Set for specific mode(s)", required: false
        }
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
	// TODO: subscribe to attributes, devices, locations, etc.
    state.lightControlActive=false
    subscribe(kodi, "status", kodiStatusChangedHandler)
}

// TODO: implement event handlers

def kodiStatusChangedHandler(evt){
	if (!evt.isStateChange()){
    	return
    }
   def currentStatus = evt.value
   log.debug "Current Status: ${currentStatus}"
   def trackTypeMap = ["object.item.videoItem.videoBroadcast":"TV Show","object.item.videoItem.movie":"Movie"]
   def rawTrackType = evt.getDevice().currentValue("trackType")
   def trackType = trackTypeMap.get(rawTrackType,"")
   switch (currentStatus){
    	case "playing":
        	if(mediaTypes.contains(trackType)){
            	if (!state.lightControlActive){
                	state.lightControlActive=true
                	state.initialLightSetting=light.currentValue("switch")
                	if (light.hasCapability("Switch Level")){
                		state.initialLightLevel=light.currentValue("level")
                	}
                }
        		if (!playingState){
            		light.off()
            	}
            	else if (light.hasCapability("Switch Level")){
            		light.setLevel(playingLevel)
            	}
            	else{
            		light.on()
            	}
            }
        	break
        case "paused":
        	if(state.lightControlActive){
            	if(!pausedState){
            		light.off()
            	}	
            	else if (light.hasCapability("Switch Level")){
            		light.setLevel(pausedLevel)
            	}
            	else{
            		light.on()
            	}
            }
        	break
        case "stopped":
       		if (state.lightControlActive){
               	if (light.hasCapability("Switch Level")){
               		light.setLevel(state.initialLightLevel)
               	}
               	switch (state.initialLightSetting){
               	case "on":
               		light.on()
                   	break
               	case "off":
               		light.off()
                   	break
               	}
           		state.lightControlActive=false
            }
        	break
        
    }
    
}