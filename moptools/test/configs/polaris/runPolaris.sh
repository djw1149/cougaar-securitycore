#!/bin/sh

set xmlPolarisConfiguration="as-10.4.xml"

#cd ~/CSI/polaris/automation/scripts03
#ruby runTestScript.rb 1a ${xmlPolarisConfiguration}

cd ~/CSI/polaris/automation/scripts03
p-run.rb security/JTG-Security-1a.rb ${xmlPolarisConfiguration}
