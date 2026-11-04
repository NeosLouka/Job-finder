# Job Finder

A Java script that discovers public job boards, retrieves job postings, filters them based on search criteria, ranks them by technical relevance, and optionally evaluates the best results using a local LLM.

The goal is simple: automate the repetitive parts of searching through company career pages and reduce a large number of job postings into a smaller, more useful list.

The script mainly handles:

- discovering public job boards
- validating whether discovered boards are still active
- fetching jobs concurrently
- normalizing jobs into a common format
- filtering irrelevant jobs
- detecting experience requirements
- ranking jobs based on their descriptions
- optionally evaluating the highest-ranked jobs with a local LLM

---

## Why I built it

I built this mainly to automate part of my own job search.

Checking company career pages manually becomes repetitive very quickly. Instead, I wanted a script that could discover job boards, collect their postings, remove obviously irrelevant jobs, and rank the remaining ones.

It also gave me an opportunity to work on problems that are more interesting than a typical CRUD application, such as:

- concurrent HTTP requests
- Java virtual threads
- rate limiting
- retry strategies
- large-scale URL discovery
- data normalization
- filtering
- text analysis
- ranking
- external API integration
- optional LLM integration

This is intentionally a script rather than a complete job-search platform. Its purpose is to automate the parts of the process that can reasonably be automated.

---

## General Flow

```text
Wayback Machine
      |
      v
Discover archived job-board URLs
      |
      v
Extract candidate boards
      |
      v
Remove duplicates
      |
      v
Validate boards
      |
      v
Save valid boards
      |
      v
Fetch jobs concurrently
      |
      v
Normalize job data
      |
      v
Apply search filters
      |
      v
Analyze experience requirements
      |
      v
Calculate relevance scores
      |
      v
Merge and sort results
      |
      v
Optional LLM evaluation
      |
      v
Return final results
```

---

## Job Board Discovery

One of the first problems is finding public company job boards.

Instead of manually maintaining a large list of companies, the script uses the **Wayback Machine CDX API** to find archived URLs belonging to supported job-board systems.

The discovery process looks roughly like this:

```text
Wayback CDX API
      |
      v
Get number of result pages
      |
      v
Fetch pages
      |
      v
Extract useful URLs
      |
      v
Parse board identifiers
      |
      v
Remove duplicates
      |
      v
Save candidate boards
```

Wayback results are retrieved page by page instead of trying to download the entire index in one request.

Some job-board domains contain a very large number of archived URLs, so one huge request can take a long time or time out.

---

## Persistent Discovery Progress

Discovery progress is saved while the script is running.

After a Wayback page is successfully processed, its candidate boards are saved and the page is marked as completed.

```text
Fetch Wayback page
      |
      v
Extract candidate boards
      |
      v
Save candidates
      |
      v
Mark page as processed
```

If the script stops, the next run can continue from the remaining pages instead of starting again from the beginning.

```text
Load processed pages
      |
      v
Skip completed pages
      |
      v
Continue discovery
```

This becomes useful when a discovery run requires many requests.

---

## Board Validation

URLs stored by the Wayback Machine are historical.

A job board that existed several years ago may no longer exist today, so discovered boards have to be validated against the live service.

The basic validation logic is:

```text
Candidate board
      |
      v
Send HTTP request
      |
      +---- 200 --------> VALID
      |
      +---- 404 --------> INVALID
      |
      +---- temporary failure
                  |
                  v
                retry
```

Temporary failures can include:

- `429 Too Many Requests`
- `5xx` server errors
- request timeouts
- connection errors

These do not automatically mean that a board is invalid.

The request is retried instead.

A `404`, on the other hand, normally means that the discovered board no longer exists.

---

## Concurrency

Most of the work performed by the script is network-bound.

It may need to:

- fetch many Wayback pages
- validate hundreds or thousands of boards
- retrieve jobs from many boards
- retrieve additional job details

Doing all of this sequentially would be unnecessarily slow.

The project therefore uses **Java virtual threads**:

```java
Executors.newVirtualThreadPerTaskExecutor();
```

Instead of processing requests one after another:

```text
request 1
   |
   v
 wait
   |
   v
request 2
   |
   v
 wait
   |
   v
request 3
```

multiple requests can spend their waiting time concurrently:

```text
request 1 --------\
request 2 ---------\
request 3 ----------> concurrent execution
request 4 ---------/
request 5 --------/
```

Virtual threads work well here because most tasks spend their time waiting for HTTP responses rather than performing CPU-heavy calculations.

---

## Limiting Concurrent Requests

Virtual threads make it cheap to create many concurrent tasks, but that does not mean the script should send unlimited requests to external services.

A `Semaphore` is used to restrict how many requests can be active at the same time.

```text
Many tasks
    |
    v
Virtual threads
    |
    v
 Semaphore
    |
    v
Only N requests active
at the same time
```

This keeps the benefits of concurrency while reducing unnecessary load and the chance of being rate limited.

---

## Fetching Jobs

After valid boards have been identified, job postings are retrieved from them.

Fetching jobs is also performed concurrently.

```text
Valid boards
    |
    +---- Board A ----> fetch jobs
    |
    +---- Board B ----> fetch jobs
    |
    +---- Board C ----> fetch jobs
    |
    +---- Board D ----> fetch jobs
```

Different job-board systems return different response structures, so their raw data cannot be used directly by the rest of the script.

The jobs are first converted into a common internal representation.

---

## Job Normalization

Different sources may use different JSON fields and structures.

The rest of the application should not need to know which source originally returned a job.

```text
Source A ----\
              \
Source B ------> JobPosting
              /
Source C ----/
```

The normalized model contains information such as:

- external ID
- source
- title
- company
- location
- job URL
- last updated date
- description
- office information

After normalization, the same filtering and ranking logic can be used for every job.

---

## Filtering

After jobs are retrieved, the script removes postings that do not match the requested search.

Filters can include:

- job title
- excluded title terms
- location
- language
- last updated date
- geographic restrictions

For example, a search may include terms such as:

```text
engineer
developer
software
backend
java
```

while excluding terms such as:

```text
senior
staff
principal
manager
director
frontend
mobile
```

Excluded title terms are checked first.

For example:

```text
Senior Backend Engineer
```

contains `backend`, but it can still be rejected because it also contains `senior`.

---

## Experience Requirement Analysis

The job description is inspected for experience requirements.

For example:

```text
2+ years of software engineering experience
```

can be interpreted as:

```text
minimum experience = 2 years
```

This makes it possible to search for jobs within a requested experience range.

For example:

```text
0-3 years
```

Jobs with clearly incompatible experience requirements can then be removed.

If the script cannot reliably detect an experience requirement, the job is kept instead of being automatically rejected.

---

## Job Ranking

After filtering, the remaining jobs are scored based on keywords found in their descriptions.

The score is not intended to determine whether a job is objectively good or bad.

It is only used to move more technically relevant jobs toward the top of the result list.

Relevant concepts can include things such as:

- backend development
- Java / JVM
- REST APIs
- distributed systems
- concurrency
- multithreading
- parallel programming
- performance engineering
- databases
- message queues
- cloud infrastructure
- containers
- observability

More important or more specific concepts can receive higher weights.

---

## Avoiding Duplicate Scoring

Longer phrases are evaluated before shorter ones.

For example:

```text
distributed systems
```

is evaluated before a shorter overlapping term such as:

```text
systems
```

After a phrase matches, that part of the description is removed before checking the remaining keywords.

This reduces accidental double scoring.

A keyword also contributes to the score only once, even if it appears multiple times in the job description.

---

## Penalty Scoring

A job can contain many technical keywords while still being unrelated to the type of role being searched for.

A separate penalty profile can therefore contain negative signals.

The final score is approximately:

```text
positive relevance score
          -
     penalty score
          =
      final score
```

This allows unrelated roles to be pushed lower in the results without relying entirely on job titles.

---

## Combining Results

Jobs collected from different sources eventually enter the same pipeline.

```text
Source A ----\
              \
Source B ------> normalized jobs
              /
Source C ----/
                  |
                  v
                filter
                  |
                  v
        experience analysis
                  |
                  v
                score
                  |
                  v
             global sort
```

The final ranking is global.

A highly relevant job from one source can therefore appear above jobs retrieved from another source.

---

## Optional LLM Evaluation

The script can optionally perform a second-stage evaluation using a locally running **Laya LLM service**.

The deterministic filtering and ranking pipeline always runs first.

```text
Retrieved jobs
      |
      v
    Filters
      |
      v
Experience check
      |
      v
Keyword ranking
      |
      v
 Take top N
      |
      v
Optional LLM evaluation
```

Only the highest-ranked jobs are sent to the LLM.

This avoids evaluating every retrieved posting with a model and keeps the normal filtering and scoring system as the main ranking mechanism.

The LLM acts as an additional relevance check.

---

## HTTP Retry Handling

Since the script performs many external HTTP requests, temporary failures are expected.

Retry handling covers cases such as:

```text
429 Too Many Requests
500 Internal Server Error
502 Bad Gateway
503 Service Unavailable
504 Gateway Timeout
request timeout
connection failure
```

Temporary failures are retried with increasing delays.

This is especially important during large discovery or validation runs, where a single failed request should not stop the entire process.

---

## Full Pipeline

```text
Wayback Machine
      |
      v
Discover archived URLs
      |
      v
Extract candidate boards
      |
      v
Deduplicate
      |
      v
Validate boards
      |
      v
Save valid boards
      |
      v
Fetch jobs concurrently
using virtual threads
      |
      v
Normalize jobs
      |
      v
Apply filters
      |
      v
Analyze experience requirements
      |
      v
Calculate relevance scores
      |
      v
Apply penalties
      |
      v
Merge results
      |
      v
Global sorting
      |
      v
Take top results
      |
      +-------------------+
      |                   |
      v                   v
   No LLM              Use LLM
      |                   |
      v                   v
Return results      Evaluate top N
                          |
                          v
                    Return results
```

---

## Tech Stack

- Java
- Spring Boot
- Maven
- Java `HttpClient`
- Java Virtual Threads
- Semaphores
- Wayback Machine CDX API
- REST APIs
- Optional local LLM integration
