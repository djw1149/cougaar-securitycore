
# This file provides web requests to the hosts, etc.

require 'security/lib/scripting'

class ExperimentFramework
   def checkHostTomcatServers
      port = run.society.cougaar_port
      hosts = run.society.agents
      checkTomcatServers(hosts, port)
   end

end
