/*
 * Arduino Freddo/DTalk library
 * (c)2014 ArkaSoft LLC - MIT License
 */

#pragma once

#include "Arduino.h"

#include "JsonParser.h"
#include "JsonGenerator.h"

class DTalk
{
public:
	DTalk(Stream &s, ArduinoJson::Parser::JsonParserBase &parser, int bufferSize);
	virtual ~DTalk();
	
	ArduinoJson::Parser::JsonObject parseBytesUntil(char c);
	
	void sendResponse(ArduinoJson::Parser::JsonObject &req, bool val);
	void sendResponse(ArduinoJson::Parser::JsonObject &req, int val);
	void sendResponse(ArduinoJson::Parser::JsonObject &req, double val);
	void sendResponse(ArduinoJson::Parser::JsonObject &req, char *val);
	
	void sendErrorResponse(ArduinoJson::Parser::JsonObject &req, int errCode, char* errMsg);
	
	void fireEvent(char *event, bool params);
	void fireEvent(char *event, int params);
	void fireEvent(char *event, double params);
	void fireEvent(char *event, char *params);
	void fireEvent(char *event, ArduinoJson::Generator::JsonValue &params);
	
	void registerService(char *name);
	
	static const char * DTALK_VERKEY;
	static const char * DTALK_VERVAL;
	static const char * DTALK_FROM;
	static const char * DTALK_TO;
	static const char * DTALK_ID;
	static const char * DTALK_SERVICE;
	static const char * DTALK_ACTION;
	static const char * DTALK_PARAMS;
	static const char * DTALK_RESULT;
	static const char * DTALK_ERROR;
	static const char * DTALK_ERRCODE;
	static const char * DTALK_ERRMSG;
	
	static const int INVALID_JSON     = -32700;
	static const int INVALID_REQUEST  = -32600;
	static const int ACTION_NOT_FOUND = -32601;
	static const int INVALID_PARAMS   = -32602;
	static const int INTERNAL_ERROR   = -32603;
	static const int REQUEST_TIMEOUT  = -32699;

private:
	Stream &DTalkStream;
	ArduinoJson::Parser::JsonParserBase DTalkParser;

	char *buffer;
	int bufferSize;
};

