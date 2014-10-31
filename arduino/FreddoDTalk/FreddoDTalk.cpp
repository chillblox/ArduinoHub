/*
 * Arduino Freddo/DTalk library
 * (c)2014 ArkaSoft LLC - MIT License
 */

#include "FreddoDTalk.h"
#include "HardwareSerial.h"

const char * DTalk::DTALK_VERKEY  = "dtalk";
const char * DTalk::DTALK_VERVAL  = "1.0";
const char * DTalk::DTALK_FROM    = "from";
const char * DTalk::DTALK_TO      = "to";
const char * DTalk::DTALK_ID      = "id";
const char * DTalk::DTALK_SERVICE = "service";
const char * DTalk::DTALK_ACTION  = "action";
const char * DTalk::DTALK_PARAMS  = "params";
const char * DTalk::DTALK_RESULT  = "result";
const char * DTalk::DTALK_ERROR   = "error";
const char * DTalk::DTALK_ERRCODE = "code";
const char * DTalk::DTALK_ERRMSG  = "message";

DTalk::DTalk(Stream &s, ArduinoJson::Parser::JsonParserBase &parser, int bufferSize) 
	: DTalkStream(s), DTalkParser(parser), bufferSize(bufferSize)
{
	buffer = (char *)calloc(bufferSize + 1, sizeof(char));
}

DTalk::~DTalk()
{
	free(buffer);	
}

ArduinoJson::Parser::JsonObject DTalk::parseBytesUntil(char c) {
	int len = DTalkStream.readBytesUntil(c, buffer, bufferSize);
	buffer[len] = '\0';
	return DTalkParser.parse(buffer);
}

void DTalk::sendResponse(ArduinoJson::Parser::JsonObject &req, bool val)
{
	char *id = req[DTALK_ID];
	char *to = req[DTALK_FROM];
	
	ArduinoJson::Generator::JsonObject<3> result;
	result[DTALK_SERVICE] = id;
	result[DTALK_RESULT] = val;
	if (to) result[DTALK_TO] = to;
	
	DTalkStream.println(result);
}

void DTalk::sendResponse(ArduinoJson::Parser::JsonObject &req, int val)
{
	char *id = req[DTALK_ID];
	char *to = req[DTALK_FROM];
	
	ArduinoJson::Generator::JsonObject<3> result;
	result[DTALK_SERVICE] = id;
	result[DTALK_RESULT] = val;
	if (to) result[DTALK_TO] = to;
	
	DTalkStream.println(result);
}

void DTalk::sendResponse(ArduinoJson::Parser::JsonObject &req, double val)
{
	char *id = req[DTALK_ID];
	char *to = req[DTALK_FROM];
	
	ArduinoJson::Generator::JsonObject<3> result;
	result[DTALK_SERVICE] = id;
	result[DTALK_RESULT] = val;
	if (to) result[DTALK_TO] = to;
	
	DTalkStream.println(result);
}

void DTalk::sendResponse(ArduinoJson::Parser::JsonObject &req, char *val)
{
	char *id = req[DTALK_ID];
	char *to = req[DTALK_FROM];
	
	ArduinoJson::Generator::JsonObject<3> result;
	result[DTALK_SERVICE] = id;
	result[DTALK_RESULT] = val;
	if (to) result[DTALK_TO] = to;
	
	DTalkStream.println(result);
}

void DTalk::sendErrorResponse(ArduinoJson::Parser::JsonObject &req, int errCode, char* errMsg)
{
	char *id = req[DTALK_ID];
	char *to = req[DTALK_FROM];
	
	ArduinoJson::Generator::JsonObject<2> error;
	error[DTALK_ERRCODE] = errCode;
	if (errMsg) error[DTALK_ERRMSG] = errMsg;

	ArduinoJson::Generator::JsonObject<3> result;
	result[DTALK_SERVICE] = id;
	result[DTALK_ERROR] = error;
	if (to) result[DTALK_TO] = to;

	DTalkStream.println(result);
}

void DTalk::fireEvent(char *event, bool params)
{
	ArduinoJson::Generator::JsonObject<2> result;
	result[DTALK_SERVICE] = event;
	result[DTALK_PARAMS] = params;
	
	DTalkStream.println(result);
}

void DTalk::fireEvent(char *event, int params)
{
	ArduinoJson::Generator::JsonObject<2> result;
	result[DTALK_SERVICE] = event;
	result[DTALK_PARAMS] = params;
	
	DTalkStream.println(result);
}

void DTalk::fireEvent(char *event, double params)
{
	ArduinoJson::Generator::JsonObject<2> result;
	result[DTALK_SERVICE] = event;
	result[DTALK_PARAMS] = params;
	
	DTalkStream.println(result);
}

void DTalk::fireEvent(char *event, char *params)
{
	ArduinoJson::Generator::JsonObject<2> result;
	result[DTALK_SERVICE] = event;
	result[DTALK_PARAMS] = params;
	
	DTalkStream.println(result);
}

void DTalk::fireEvent(char *event, ArduinoJson::Generator::JsonValue &params)
{
	ArduinoJson::Generator::JsonObject<2> result;
	result[DTALK_SERVICE] = event;
	result[DTALK_PARAMS] = params;
	
	DTalkStream.println(result);
}

void DTalk::registerService(char *name) {
	ArduinoJson::Generator::JsonObject<3> request;
	//request[DTALK_ID] = name;
	request[DTALK_ACTION] = "register";
	request[DTALK_PARAMS] = name;
	
	DTalkStream.println(request);
}
