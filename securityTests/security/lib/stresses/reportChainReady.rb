

class TestReportChainReady < SecurityStressFramework
  def initialize(run)
    super(run)
    @run = run
    @expectedSubordinates = Hash.new
    @foundSubordinates    = Hash.new
    @bbPlugin = "org.cougaar.core.security.test.BlackBoardCollectorPlugin"
    @stressid = "ReportChainReady Detector"
  end

  def getStressIds
    return [@stressid]
  end

  def beforeStartedSociety
    @run.society.each_agent(true) do |agent|
      facetval = agent.get_facet(:superior_org_id)
      if facetval != nil
        subordinate = agent.name
        superior    = facetval
        if  @expectedSubordinates[superior] == nil
          @expectedSubordinates[superior]  = [] 
        end
        if !(@expectedSubordinates[superior].include?(subordinate))
          @expectedSubordinates[superior].push(subordinate)
        end
      end
    end
    @run.comms.on_cougaar_event do |event|
      eventCall(event)
    end
  end

  def afterReportChainReady
    badChains = getBadChains ["OSD.GOV"]
    if badChains.empty?
      saveResult true, "ReportChainReady Achieved"
    else
      saveResult false, "ReportChainReady failed"
      badChains.each do |chain|
        saveAssertion stressid, 
                      "ReportChainReady failed at subordinate #{chain.last}"
        explanation = "Subordinate chain = "
        chain.each do |agent|
          explanation += "#{agent} -> "
        end
        explanation += "All good below"
        saveAssertion stressid, explanation
      end
    end
  end

  def getBadChains stack
    superior  = stack.last
    expected  = @expectedSubordinates[superior]
    found     = @foundSubordinates[superior]
    badChains = []
    if expected == nil || expected.empty?
      return badChains
    end
    expected.each do |expectedSub|
      if ((found == nil) || (! found.include? expectedSub))
        newStack     = stack.clone.push expectedSub
        newBadChains = getBadChains newStack
        if newBadChains.empty?
          badChains.push newStack
        else
          badChains.concat newBadChains
        end
      end
    end
    return badChains
  end

  def eventCall(event)
    regexp = /Interception: ReportForDuty with role : <(.*)> : <(.*)> : (.*)/
    parsed = regexp.match(event.data)
    if parsed != nil
      subordinate = parsed.to_a[1].split(" ").last
      superior    = parsed.to_a[2].split(" ").last
      role        = parsed.to_a[3]
      if (@foundSubordinates[superior] == nil)
        @foundSubordinates[superior] = []
      end
      @foundSubordinates[superior].push(subordinate)
    end
  end
end