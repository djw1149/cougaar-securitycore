require 'security/lib/message_util'

class Stress3c1 < SecurityStressFramework
   def initialize(run)
     super(run)
   end

   def getStressIds()
    return ["Stress3c1", ]
  end

#  def preConditionalGLSConnection
  def postConditionalNextOPlanStage
    saveResult(!agentExists('NO-SSL-ATTACK-NODE'),
               'Stress3c1', "Non-SSL Communication");
  end # postConditionalNextOPlanStage

  def preTransformSociety
    host = installAttackHost
    host.add_node("NO-SSL-ATTACK-NODE") { |node|
      node.add_facet('attacker' => 'NonSSL')
      node.override_parameter('-Dorg.cougaar.lib.web.http.port', '8820');
      @mynode = node
    }
  end

  def postTransformSociety
    # remove SSL from the node that shouldn't have it

    sslRMI      = "org.cougaar.core.mts.SSLRMILinkProtocol"
    rmiProtocol = "org.cougaar.core.mts.RMILinkProtocol"
    insertion   = "Node.AgentManager.Agent.MessageTransport.Component"

    @mynode.remove_component(sslRMI)
    @mynode.add_component { |component|
      component.classname = rmiProtocol
      component.insertionpoint = insertion
    }
  end # postTransformSociety

=begin  
  def postLoadSociety
    # Now inserted through a rule
    installSendMessageServlet
  end
=end
end # Stress3c1

# agent should successfully send a message
class Stress3a101 < SecurityStressFramework
  def initialize(run)
    super(run)
    @answered = false
    @saved = false
  end

  def getStressIds() 
    return ["Stress3a101"]
  end
=begin
  def postLoadSociety
    # Now inserted through a rule
    installSendMessageServlet
  end
=end

  def postConditionalNextOPlanStage
    # find two random agents. They should be allowed to communicate
    # if they are not in the attack node
    agent1 = nil
    agent2 = nil
    @run.society.each_agent { |agent|
      if (agent.node.get_facet("attacker") == nil)
        if (agent1 == nil) 
          agent1 = agent
        else
          if (agent1.node != agent.node) 
            agent2 = agent
            break
          end
        end
      end
    }

    testMessageSuccess(agent1.name, agent2.name, 
                       'Stress3a101', "Send message successfully")
  end # postConditionalNextOPlanStage
end # Stress3a101

