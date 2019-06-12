# audio-port-fetcher 0.1.0

A JBrowser-based crawler to download audio programs from audioport.org.

## Who is this for?

AudioPort subscribers with the means to set up an automated job to
fetch audio episode files on a regular basis.

## Usage

    $ java -jar audio-port-fetcher-0.1.0-standalone.jar <action> [options] <program-codes>

### Actions

* `fetch`: Downloads an audio file for the specified programs.

### Options

* `-c`, `--config`: Specify an alternate configuration file.

## Configuration

A simple configuration file carries the credential information and
a list of all the programs that could be fetched.

In this example below, a radio station wants to fetch two programs
from audioport.org: "Ralph Nader Radio Hour" and "The Michael Slate Show":

``` clojure
{
 :credentials
 {
  :username "your-audioport.org-username"
  :password "your-audioport.org-password"
  }
 :programs
 {
  :rn
  {
   :pub_title "Ralph Nader Radio Hour"
   }
  :ms
  {
   :pub_title "The Michael Slate Show"
   }
  }
 }
}
```

The abbreviated codes selected are user-selectable but the
`:pub_title` field must exactly match (including capitalization) the
title of the program on audioport.org.

`audio-port-fetcher` expects to find the configuration at
`~/.audioportfetcher`.

## Examples

`audio-port-fetcher` does not automatically fetch all the
programs. Rather, it is invoked with one of the abbreviated program
codes from the configuration file. Given the sample above, to fetch
the latest episode of Ralph Nader's program, running the command:

    $ java -jar audio-port-fetcher-0.1.0-standalone.jar fetch rn

Will produce the MP3 of the latest program in the current
filesystem's directory.
