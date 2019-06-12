# Change Log
All notable changes to this project will be documented in this file. This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/).

## 0.1.1 - 2019-06-12
### Changed
* Configuration file now expected to be `~/.audioportfetcher`.
* Extract filename from response header. Fallback to sha-256 if not returned.

### Added
* Program argument `-c` option to specify a different configuration
  file to use.
* Basic configuration format check.
* Logout from audioport.org when finished.

### Fixed
* Program code arguments can now include a leading colon.
* Allow processing of multiple program codes.

## 0.1.0 - 2019-06-09
Initial working version with basic functionality to fetch latest episode.
