#Cougaar Actions to Start and Stop analysis of network traffic
#
#	Action StartEtherealAnalysis start tethereal and dumps the results to a file.
#
#	Action StopEtherealAnalysis stops the process created by executing tethereal.
#	The process id is stored in the global variable $pid.	Next, tetheral is run 
#	again to analyze the results and dump them into another file. 
#
#
module Cougaar
        module Actions
         class StartEtherealAnalysis < Cougaar::Action
                         def initialize(run)
                                super(run)
                                print "Enter Hostname: "
                                $hostname = gets.chomp
                                @run = run
                     end
                     def perform
                        outputfilename="#{@run.name}_analysis.in"
                        puts "Output of raw network data #{outputfilename}"
                        puts "Removing old file if present"
                        commandsyntax="ssh #{hostname} ./runTethereal.sh #{outputfilename}"
                        puts "Executing #{commandsyntax}"
                        $pid = fork{
                                 puts "Executing #{commandsyntax}"
                                 exec(commandsyntax)
                        }
                        puts "PID: #{$pid}"
                     end
                end

                class StopEtherealAnalysis  < Cougaar::Action
                         def initialize(run)
                                super(run)
                                @run = run
                     end
                     def perform
                        system "ssh -t #{hostname} ./stopTehtereal"
                        infile="#{@run.name}_analysis.in"
                        outfile="#{@run.name}_analysis.out"
                        puts "Analyze file: #{infile}"
                        puts "Create file: #{outfile}"
                        commandsyntax="ssh -t #{hostname} ./readTethreal.sh #{infile} #{outfile}"
                        exec(commandsyntax)
                        puts "Output written to #{outfile} on #{hostname}"
                     end
                end
        
        end
end