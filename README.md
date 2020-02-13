# storage-service

[![Build Status](https://travis-ci.org/wellcomecollection/storage-service.svg?branch=master)](https://travis-ci.org/wellcomecollection/storage-service)

This repo contains the code for our storage service, which holds Wellcome Collection's digital archive.
The archive holds our digital collections, including:

-   [*Born-digital archives*](https://en.wikipedia.org/wiki/Born-digital): files that started life as digital objects, say an Excel spreadsheet or an email
-   [*Digitised material*](https://en.wikipedia.org/wiki/Digitization): digital reproductions of physical objects, like a scanned pamphlet or a photographed book
-   Other files that we want to preserve for a long time



## Principles and requirements

Any storage system we use has to provide **access** to our digital collections, and **preserve** them for future use.

We had the following principles in mind when we were designing and building this service:

-   **Files should be portable.**
    We aren't saving files for weeks or months, we're preserving them for *years*.
    The files themselves will likely outlive any particular software stack, so we want to organise them in a way that remains useful long after all our code is gone.

-   **The storage layout should be human-readable.**
    To maximise portability, the files on disk should be laid out in a human-readable way.
    It should be possible for somebody to understand the structure of the archive with only the files, even without any of our code or databases.

    ```
    digitised/
      └── b31497652/
            ├── v1/
            │    ├── bagit.txt
            │    ├── bag-info.txt
            │    └── data/
            │          ├── b31497652_0001.jp2
            │          └── ...
            └── v2/
                 └── ...
    ```

-   **We use open source and open standards as much as possible.**
    This means it's more likely tools will continue to exist that can understand the files in the archive, even if all of our code is no longer in use.

    The unit of storage is "bags" in the [BagIt packaging format](https://tools.ietf.org/html/rfc8493).
    BagIt is an open standard developed by the Library of Congress.



## High-level design

<figure>
  <img src="high_level_design.svg">
  <figcaption>
    Icons:
    <a href="https://thenounproject.com/term/paper-bag/28579/">Paper bag</a> by Dorian Dance. From The Noun Project, CC BY.
    <a href="https://thenounproject.com/term/process/2473979/">Process</a>, by Alice Design. From The Noun Project, CC BY.
    <a href="https://aws.amazon.com/architecture/icons/">Bucket</a>, from the AWS Architecture icons.
  </figcaption>
</figure>

The user uploads a "bag" in the BagIt packaging format.

The storage service unpacks the bag, and verifies the checksums in the BagIt manifest.
If the checksums match the files, it creates three copies of the bag:

*   One in Amazon S3: the warm copy, for day-to-day access
*   One in Amazon Glacier: a backup copy
*   One in Azure Blob Storage: a different cloud provider and geographic location, for extra redundancy (not implemented as of February 2020)

It then re-verifies the three copies of the bag to ensure they were stored correctly, and reports the bag as successfully stored to the user.

**The storage service stores exactly the bytes you give it; no more, no less.**
It does not do any introspection of the bag contents, or change its behaviour based on the files a bag contains.



## Identifiers

For long-term portability, it's important to use human-readable and understandable identifiers (not [UUIDs](https://en.wikipedia.org/wiki/Universally_unique_identifier)).
The storage service does not create its own identifiers for bags -- the user has to provide their own identifier.
This allows bags to be matched to records in systems outside the storage service (for example, the library catalogue).

Bags in the storage service have a three-part identifier:

*   **Space:** the broad category of a bag.
    Examples: `digitised`, `born-digital`.

*   **External identifier:** the identifier of a bag within a space.
    This is typically an identifier from another system, which matches this bag to that record.
    Examples: `b31497652`, `PP/CRI/A/2`.

*   **Version:** an auto-incrementing version assigned by the storage service.
    This tracks distinct versions of a (space, external identifier) pair.
    Examples: `v1`, `v2`, `v3`.

These three parts can be combined into a single string, which uniquely identifies a bag; for example `digitised/b31497652/v2`.
This identifier is also the path to the root of the bag inside our storage buckets.



## Read more

If you'd like to learn more about the storage service:

-   [**Building Wellcome Collection’s new archival storage service**](https://stacks.wellcomecollection.org/building-wellcome-collections-new-archival-storage-service-3f68ff21927e) explains more of the ideas and principles behind the storage service, and how we successfully migrated all of our digitised content into the system.

-   [**How we store multiple versions of BagIt bags**](https://stacks.wellcomecollection.org/how-we-store-multiple-versions-of-bagit-bags-e68499815184) explains how we've added versioning atop the BagIt spec, to track distinct versions of bags.

We've also written about some of the code used in the storage service:

-   [**Creating a locking service in a Scala type class**](https://alexwlchan.net/2019/05/creating-a-locking-service-in-a-scala-type-class/).
    When we're writing a bag to an S3 bucket, we want to be sure that only one process is running at a time -- we don't want multiple processes writing to the same bucket, and potentially writing conflicting data.
    Because S3 doesn't have support for this sort of locking, we have to implement it in our code.

-   [**Iterating over the entries of a compressed archive (tar.gz)**](https://alexwlchan.net/2019/09/unpacking-compressed-archives-in-scala/) (Scala).
    When a user uploads a bag, it has to be packaged as a tar.gz file.
    This explains the code we use to unpack those archives.

-   [**Streaming large objects from S3 with ranged GET requests**](https://alexwlchan.net/2019/09/streaming-large-s3-objects/) (Java/Scala).
    If you're trying to read a large object from S3, the connection may drop before you read all the bytes.
    Using ranged GET requests allows you to request a piece at a time, and then you can use Java's `SequenceInputStream` to combine them together.

-   [**Working with really large objects in S3**](https://alexwlchan.net/2019/02/working-with-large-s3-objects/) (Python).
    A wrapper class that adds a couple of methods to the file-like objects returned by the AWS SDK for Python, allowing you to use it with the zipfile module and the like.


## License

All the code in this repo is available under an MIT license.
