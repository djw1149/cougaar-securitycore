#!/usr/bin/ruby

require "#{$CIP}/operator/security/checkTraceAlt.rb"

module Cougaar
  module Actions
    # Run checkTrace.rb while the society is running and stops the society if
    # there is something wrong with it. This is especially useful during
    # automated testing.
    class HealthCheck < Cougaar::Action
      def initialize(run, sleep_time=3.minutes)
        super(run)
        @sleep_time = sleep_time
      end
      def perform
        checkSocietyHealth()
      end

      def checkSocietyHealth
        # In a background thread:
        # 1) Run checkTraceAlt.rb script
        # 2) If there is a problem, stop the society.
        # 3) If there is no problem, sleep for 5 minutes,
        #    then goto step 1.
        thread = Thread.fork {
         while (true)
           sleep @sleep_time
           testPassed = processLogFiles() 
           if !testPassed
             ::Cougaar.logger.warn "Test failure"
             `killall -9 ruby`
           end
         end
        }
      end

    end
  end
end
