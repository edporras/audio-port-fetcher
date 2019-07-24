# audio-port-fetcher 0.1.2

A JBrowser-based crawler to download program audio files from
audioport.org.

## Who is this for?

AudioPort subscribers with the means to set up an automated job to
fetch audio episode files periodically.

## Installation

1. Install [Java SE Runtime Environment 8](https://www.java.com/en/download/).

2. Download the latest `audio-port-fetcher` standalone JAR from the
[release page](https://github.com/edporras/audio-port-fetcher/releases/latest)
and save it somewhere you can later find it.

3. Create a configuration file with your credentials and the list of
   programs you'd like to fetch. See the Configuration section below.

### Configuration

The configuration file contains the credential information and
the list of all the programs that could be fetched.

With this example below, a radio station can fetch two programs from
audioport.org: "Ralph Nader Radio Hour" and "The Michael Slate Show":

``` clojure
{
 :credentials {
  :username "your-audioport.org-username"
  :password "your-audioport.org-password"
  }
 :programs {
  :rn {
   :pub_title "Ralph Nader Radio Hour"
   }
  :ms {
   :pub_title "The Michael Slate Show"
   }
  }
 }
}
```

You can choose the abbreviated codes as you'd like but they can't have
spaces. Also, the `:pub_title` field should match (including
capitalization) the title of the program on audioport.org.

`audio-port-fetcher` expects to find the configuration at
`~/.audioportfetcher` but you can specify a config file in a different
location or with a different name when running the program.

(You can [download or copy / paste this sample file](doc/myconfig.edn)
but you must update the file with your audioport user name and
password).

## Usage

You can run the program from the command-line using the following
format:

```
java -jar audio-port-fetcher-0.1.2-standalone.jar <action> [options] <program-codes>
```

#### Actions

* `fetch`: Downloads an audio file for the specified programs.

#### Options

* `-c`, `--config`: Specify an alternate configuration file.
* `-d`, `--date`: Specify a date for fetching a program's episode.

#### Program Codes

`audio-port-fetcher` does not automatically fetch all the programs
found in the configuration. Instead, you specify the ones to fetch
when running it. The abbreviated code refers to the code with a
leading colon at the beginning of each program block in the
configuration file (e.g., `:rn`, `:ms`).

### Examples

Given the sample configuration above, you can fetch the latest episode of
the Ralph Nader Radio Hour as so:

```console
java -jar audio-port-fetcher-0.1.2-standalone.jar fetch :rn
```
If your configuration file is not in the expected location, you can
tell the program using the `-c` option:

```console
java -jar audio-port-fetcher-0.1.2-standalone.jar fetch -c myconfig.edn :rn
```
To fetch the latest episodes of more than one program at a time, pass
multiple codes:

```console
java -jar audio-port-fetcher-0.1.2-standalone.jar fetch :rn :ms
```
To fetch an episode using a known release date, use the `-d` option
with the date in `YYYY-MM-DD` format:

```console
java -jar audio-port-fetcher-0.1.2-standalone.jar fetch -d 2019-07-10 :ms
```
In each case, the MP3 of the latest program will be downloaded to the
current directory.
