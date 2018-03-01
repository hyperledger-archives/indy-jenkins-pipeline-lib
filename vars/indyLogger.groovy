import groovy.transform.Field

@Field static final Map logLevels = [
    TRACE: 10,
    DEBUG: 20,
    INFO: 30,
    WARNING: 40,
    ERROR: 50
]

@Field static private logLevelMin = null // TODO make it really private

static def setLogLevel(logLevel) {
    logLevelMin = logLevels[logLevel]
}

def trace(message, prefix=null) {
    log(message, 'TRACE', prefix)
}

def debug(message, prefix=null) {
    log(message, 'DEBUG', prefix)
}

def info(message, prefix=null) {
    log(message, 'INFO', prefix)
}

def warning(message, prefix=null) {
    log(message, 'WARNING', prefix)
}

def error(message, prefix=null) {
    log(message, 'ERROR', prefix)
}

def log(message, logLevel, prefix) {
    if (logLevels[logLevel] >= logLevelMin) {
        echo "${logLevel}: " + (prefix ? "(${prefix}) " : "") + "${message}"
    }
}
