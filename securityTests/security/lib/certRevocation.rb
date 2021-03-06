
require 'security/lib/common_security_rules'

class CertRevocation
  attr_accessor :management
  attr_accessor :canodes
  attr_accessor :normal_nodes
  
  def initialize
    @management = {}
    @canodes = {}
    @normal_nodes = {}
    
    distinguishNodes
  end
  
  def getManagement
    return management
  end
  
  def distinguishNodes
    run.society.each_node do |node|
      #        puts "working on #{node.name}"
      untouchable = false
      node.each_facet do |facet|
        if facet['role'] == $facetManagement
          # do not revoke management agents
          management[node.name] = node
          untouchable = true
        end
        if facet['role'] =~ /CertificateAuthority/
          canodes[node.name] = node
          untouchable = true
          break
        end
      end
      
      if !untouchable
        normal_nodes[node.name] = node
      end
    end
    
    
  end
  
  def selectNode
    index = rand(normal_nodes.size)
    return normal_nodes.values[index]
  end
  
  def selectAgent
    node = selectNode
    return selectAgentFromNode(node)
  end
  
  def selectAgentFromNode(node)
    index = rand(node.agents.size)
    a = node.agents[index]
    if a != nil
      return a.name
    end
    return nil
  end
  
  def revokeNode(node) 
    #saveUnitTestResult("Stress5k104", "revoking node #{node.name}")
    agent = node.agents[0]
    caDomains = run.society.agents[agent.name].caDomains
    return revoke(node, caDomains)
  end
  
  def revokeAgent(agent)
    caDomains = agent.caDomains
    return revoke(agent, caDomains)
  end
  
  def revoke(agent, caDomains)
    CaDomains.instance.getActualEntities
    caManager = caDomains[0].signer
    cadn = caDomains[0].cadn
    #puts "cadn #{cadn}"
    
    url = "#{caManager.uri}/CA/RevokeCertificateServlet"
    # role = getParameter(ca.node, /security.role/, nil)
    
    #  dn_names = getDistinguishNames(caDomains)
    #  dnname = dn_names[agent]
    ###              dnname = caDomains[0].distinguishedNames[agent]
    #puts agent
    #doIrb
    dnname = agent.distinguishedName
    #puts "dnname #{dnname}"
    if dnname == nil
      saveAssertion "Stress5k104", "Trying to revoke #{agent.name} - Unable to find distinguished name, Cannot revoke"
      #exit
      return false
    end
    
    params = ["cadnname=#{CGI.escape(cadn)}", "distinguishedName=#{dnname}"]
    ##    set_auth('george', 'george')
    #puts "params: #{params}"
    response = postHtml(url, params)
    
    #puts "response.code #{response.code}, body #{response.body}"
    #              puts "revocation response #{response.body.to_s}"
    if response.body.to_s =~ /Success/
      saveAssertion("Stress5k104", "successfully revoke agent: #{agent.name}")
      #puts "Successfully revoked #{agent}"
      return true
    else
      saveAssertion("Stress5k104", "Unable to revoke agent: #{agent.name}")
      #puts "Revoke #{agent} failed"
    end
    return false
  end
  
  # for expiration
  def installExpirationPlugin(node)  
    # node to ignore expired cert
    node.add_component do |c|
      #      c.classname = 'org.cougaar.core.servlet.SimpleServletComponent'
      c.classname = 'org.cougaar.core.security.certauthority.CaServletComponent'
      c.add_argument("org.cougaar.core.security.crypto.servlet.MakeCertificateServlet")
      c.add_argument("/MakeCertificateServlet")
    end
    
  end
  
  #
  def requestNewCertificate(node, agent, timeString)
    
    saveAssertion("Stress5k104", "setCAExpirationAttrib #{node.name} #{agent.name} #{timeString}")
    setCAExpirationAttrib(agent, timeString)
    port = getParameter(node, /http.port/, nil)
    #url = "http://#{node.host.name}:#{port}/$#{node.name}/MakeCertificateServlet"
    url = "#{node.uri}/$#{node.name}/MakeCertificateServlet"
    #puts "agent uri returned ---> #{agent.uri}"
    #puts "node uri returned ---> #{node.uri}"
    saveAssertion("Stress5k104", "setCAExpirationAttrib #{url} - agent uri: #{agent.uri} node uri: #{node.uri}")
    params = ["identifier=#{agent.name}"]
    saveAssertion("Stress5k104", "Invoking #{url} #{params}")
    response = postHtml(url, params)
    raise "Failed to get new certificate. Error #{response.body.to_s}" unless response.body.to_s =~ /Success/
    
  end
  
  def setNodeExpiration(node, timeString)
    puts "expiring #{node.name}"
    agent = node.agents[0]
    requestNewCertificate(node, agent, timeString)
  end
  
  def setAgentExpiration(agent, timeString)
    agent = run.society.agents[agent] if agent.kind_of?(String)
    setCAExpirationAttrib(agent, timeString)
    removeAgentIdentities(agent)
  end
  
  def removeAgentIdentities(agent)
    agent = run.society.agents[agent] if agent.kind_of?(String)
    puts "remove identities of #{agent.name}"
    node = agent.node
    
    port = getParameter(node, /http.port/, nil)
    #url = "http://#{node.host.name}:#{port}/$#{node.name}/MakeCertificateServlet"
    url = "#{node.uri}/$#{node.name}/MakeCertificateServlet"
    params = ["identifier=#{agent.name}"]
    response = postHtml(url, params)
    unless response.body.to_s =~ /Success/
      logWarningMsg "RemoveAgentIdentities - #{response.body}"
    end
    raise "Failed to get new certificate. Error #{response.body.to_s}" unless response.body.to_s =~ /Success/
    
  end

  def setCAExpirationAttrib(agent, timeString)
    if (agent == nil)
      saveAssertion("Stress5k104",
           "setCAExpirationAttrib: Unable to find agent: #{agent.name}")
      raise "setCAExpirationAttrib: Unable to find agent: #{agent.name}"
    end
    caDomains = agent.caDomains
    caManager = caDomains[0].signer
    cadn = caDomains[0].cadn
    #puts "cadn #{cadn}"
    
    url = "#{caManager.uri}/CA/CAInfoServlet"
    params = ["howLong=#{timeString}"]
    response = postHtml(url, params)
    raise "Failed to set CA expiration: #{response.body.to_s}" unless response.body.to_s =~ /Changed/
  end
  
  # get parameter from node given param name
  def getParameter(node, paramName, default)
    node.each_parameter do |p|
      (name, value) = p.to_s.split('=')
      return value if name =~ paramName
    end
    
    puts "No parameter found for #{paramName} on #{node.name}"
    return default
  end
end

