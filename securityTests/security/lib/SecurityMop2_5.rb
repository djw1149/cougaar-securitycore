require 'security/lib/AbstractSecurityMop'
require 'security/lib/SecurityMop2_4'

class SecurityMop2_5 < AbstractSecurityMop
  include Singleton
  
  def initialize()
    #   super(run)
    @name = "2-5"
    @descript = "Percentage of all designated user actions that are recorded"
  end

  def getStressIds()
    return ["SecurityMop2.5"]
  end


  def to_s
    logged = SecurityMop2_4.instance.numActionsLogged
    total = SecurityMop2_4.instance.numLoggableActions
    answer = 100
    answer = logged / total unless total == 0
    return "policy actions: (logged)#{logged}/(total)#{total} = #{answer}"
  end

  def isCalculationDone
    return SecurityMop2_4.instance.isCalculationDone
  end

  def calculate
    Thread.fork do
      calculateThread
    end
  end

  def calculateThread
    begin
      totalWaitTime=0
      maxWaitTime = SecurityMop2_4.instance.maxWaitTime
      sleepTime=60.seconds
      while ((SecurityMop2_4.instance.getPerformDone == false) && (totalWaitTime < maxWaitTime))
        logInfoMsg "Sleeping in Calculate of SecurityMop2.5 . Already slept for #{totalWaitTime}" if totalWaitTime > 0
        sleep(sleepTime) # sleep
        totalWaitTime += sleepTime
      end
      if((totalWaitTime >= maxWaitTime) && (SecurityMop2_4.instance.getPerformDone == false))
        @summary = "MOP 2.5 did not complete."
        saveResult(false, "SecurityMop2.5", "Timeout tests incomplete") 
        saveAssertion("SecurityMop2.5", "Save results for SecurityMop2.5 Done Result failed ")
        return
      elsif (SecurityMop2_4.instance.getPerformDone == true)
        @score = SecurityMop2_4.instance.score5
        logged = SecurityMop2_4.instance.numActionsLogged
        total = SecurityMop2_4.instance.numLoggableActions
        if total == 0
          if @numAccessAttempts == 0
            @summary = "There weren't any access attempts."
          else
            @summary = "There weren't any access attempts which needed to be logged"
            @summary = @summary + " (there were #{SecurityMop2_4.instance.numTimeouts} timeouts)" if SecurityMop2_4.instance.numTimeouts > 0
            @summary = @summary + "."
        end
        else
          # note: these two values are swapped, but are fixed on the analysis side
          @summary = "There were #{total} servlet access attempts, #{logged} were correct.\n"
        end
        @raw = SecurityMop2_4.instance.raw5
        @info = SecurityMop2_4.instance.html5
        csisummary = "SecurityMop2.5( User action audit )\n<BR> Score :#{@score}</BR>\n" 
        csisummary << "#{@summary} \n"
        #csisummary << "#{@info}"
        success = false
        if (@score == 100)
          success = true
        end
        saveResult(success, 'SecurityMop2.5', csisummary)
        saveAssertion("SecurityMop2.5", @info)
        saveAssertion("SecurityMop2.5", "Save results for SecurityMop2.5 Done" )
      end
    rescue Exception => e
      puts "error in 2.4 calculate "
      puts "#{e.class}: #{e.message}"
      puts e.backtrace.join("\n")
    end
  end

  def scoreText
    if @summary =~ /^There weren/
      return NoScore
    else
      return @score
    end
  end
end
    
