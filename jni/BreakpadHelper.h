/*
 * BreakpadHelper.h
 *
 *  Created on: 2014-2-17
 *      Author: zhangyongxiang
 */

#ifndef BREAKPADHELPER_H_
#define BREAKPADHELPER_H_

#include <string>
#include <vector>
#include "processor/simple_symbol_supplier.h"

using std::string;
struct BacktraceInfo{
	string module;
	string function;
	string source;
	int line;
};

class BreakpadHelper{
public:
	static BreakpadHelper *getInstance();
	static void destroyInstance();
	bool backtrace(const char *dump_path,
			const char *symbol_path,
			google_breakpad::SimpleSymbolSupplier::File_exists_handler exists_handler=0,
			google_breakpad::SimpleSymbolSupplier::File_read_handler read_handler=0,
			google_breakpad::SimpleSymbolSupplier::Free_memory_handler free_handler=0);
	void addBacktrace(string module, string function, string source, int line);
	std::vector<BacktraceInfo> *getBacktraceResult(){return &mBacktrace;}
private:
	static BreakpadHelper *mInstance;
	std::vector<BacktraceInfo> mBacktrace;
};


#endif /* BREAKPADHELPER_H_ */
