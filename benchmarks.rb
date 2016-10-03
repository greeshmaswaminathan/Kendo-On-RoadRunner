#!/bin/env ruby

require 'optparse'
require 'date'

BENCHMARKS = [
  "barrier",
  "classes",
  "colt",
  "crypt",
  "DacapoRR",
  "forkjoin",
  "jbb",
  "jbb-fixed",
  "jg-part3-concurrent",
  "lufact",
  "matrixmult",
  "moldyn",
  "montecarlo",
  "mtrt",
  "multiset",
  "philo",
  "queue-jg",
  "queue-qs",
  "queue-simple",
  "raytracer",
  "series",
  "sor",
  "sor-barrier",
  "sor-barrier-new",
  "sparsematmult",
  "sync",
  "tsp"
]

benchmarks = []
benchdir = ''
action = ''

OptionParser.new do |opts|
  opts.banner = "Usage: benchmarks.rb [options]"

  # e.g. /home/cs/Projects/sqt/proj/JavaWorkspace/Benchmarks
  opts.on("-P", "--benchdir PATH", "Base path for benchmarks") do |path|
    benchdir = path
  end

  opts.on("-B", "--benchmark BENCHMARK", BENCHMARKS + ["all"],
          "Specify benchmark (use 'all' to run all)",
          "Available: " + BENCHMARKS.join("\n" + (' ' * 48))) do |b|
    if b == "all"
      benchmarks = BENCHMARKS
    else
      benchmarks |= [b]
    end
  end

  opts.on("-A", "--action ACTION", [:compile, :run, :test],
          "Select the action to perform for each benchmark (compile, run or test)") do |a|
    action = a
  end

  opts.on_tail("-h", "--help", "Show this message") do
    puts opts
    exit
  end
end.parse!

if benchmarks.empty?
  puts "No benchmarks specified!"
  exit
end

rrdir = File.expand_path(File.dirname(__FILE__))

benchmarks.each do |benchmark|
  case action
  when :compile
    system "cd #{benchdir}/#{benchmark} && ./COMPILE"
  when :run
    ENV["JAVA"] = `which java`
    system "cd #{benchdir}/#{benchmark} && ./RUN"
  when :test
    ENV["PATH"] += ":#{rrdir}/build/bin"
    ENV["JVM_ARGS"] = "-Xmx4g -Xms1g"
    ENV["RR_HOME"] = rrdir
    ENV["RR_TOOLPATH"] = "#{rrdir}/jars/java-cup-11a.jar"
    ENV["TEST_ARGS"] = "-quiet -tool=S:ESO -logs=#{rrdir}/log/#{benchmark}/#{Date.today().to_s}"
    system "cd #{benchdir}/#{benchmark} && ./TEST"
  else
    puts "Unsupported action"
  end
end
