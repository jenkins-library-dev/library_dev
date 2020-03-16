#!groovy

def colorCodes() {
    GREEN  = '\033[0;32m'
    RED    = '\033[0;31m'
    NC     = '\033[0m'
}

def record(String phase, String message, String nature) {
    def phase_log = "[${phase} - ${nature}] ${message} "

    if ( nature == "INFO" ) {
        colorCodes()
        sh """
              set +x;
              echo -e "${GREEN}${phase_log}${NC}"
           """
    }

    if ( nature == "ERROR" ) {
        colorCodes()
        sh """
              set +x;
              echo -e "${RED}${phase_log}${NC}"
           """
    }
}

return this;