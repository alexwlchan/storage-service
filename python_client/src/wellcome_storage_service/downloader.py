# -*- encoding: utf-8

import abc
import os
import tarfile

try:
    from collections.abc import ABC
except ImportError:  # Python 2
    from abc import ABCMeta as ABC

from ._utils import mkdir_p


def _choose_provider(location):
    if location["provider"]["id"] == "aws-s3-ia":
        return S3InfrequentAccessProvider()
    else:
        raise RuntimeError(
            "Unsupported storage provider: %s" % location["provider"]["id"]
        )


def _all_files(storage_manifest):
    return (
        storage_manifest["manifest"]["files"] + storage_manifest["tagManifest"]["files"]
    )


def download_bag(storage_manifest, out_dir):
    """
    Download all the files in a bag to a given directory.

    :param storage_manifest: A storage manifest returned from the storage
        service, as retrieved with ``get_bag()``.
    :param out_dir: The directory to download the bag to.

    """
    location = storage_manifest["location"]
    provider = _choose_provider(location)

    for manifest_file in _all_files(storage_manifest):
        provider.download(
            out_dir=out_dir, location=location, manifest_file=manifest_file
        )


def download_compressed_bag(storage_manifest, out_path):
    """
    Download all the files in a bag to a compressed archive.

    :param storage_manifest: A storage manifest returned from the storage
        service, as retrieved with ``get_bag()``.
    :param out_path: The path to download the tar.gz to.

    """
    location = storage_manifest["location"]
    provider = _choose_provider(location)

    with tarfile.open(out_path, "w:gz") as tf:
        for manifest_file in _all_files(storage_manifest):
            fileobj = provider.get_fileobj(
                location=location, manifest_file=manifest_file
            )

            tarinfo = tarfile.TarInfo(name=manifest_file["name"])
            tarinfo.size = manifest_file["size"]

            tf.addfile(tarinfo=tarinfo, fileobj=fileobj)


class AbstractProvider(object):
    """
    Abstract class for a downloader.

    Subclasses should implement the ``_download_fileobj`` method, which
    downloads a single ``manifest_file`` from a bag's manifest at ``location``
    to a writable ``file_obj`` in binary mode.
    """

    __metaclass__ = ABC

    @abc.abstractmethod
    def get_fileobj(self, location, manifest_file):
        """
        Abstract method that should return a binary file.
        """
        pass

    def download(self, out_dir, location, manifest_file):
        out_path = os.path.join(out_dir, manifest_file["name"])

        mkdir_p(os.path.dirname(out_path))

        with open(out_path, "wb") as write_file_obj:
            read_file_obj = self.get_fileobj(
                location=location, manifest_file=manifest_file
            )

            # This process is deliberately chunked to avoid loading the
            # whole contents of a file into memory at once, when we can
            # stream lazily and keep the memory footprint down.
            while True:
                next_chunk = read_file_obj.read(8096)
                if not next_chunk:
                    break
                write_file_obj.write(next_chunk)


class S3InfrequentAccessProvider(AbstractProvider):
    def __init__(self):
        import boto3

        self.s3_client = boto3.client("s3")
        super(S3InfrequentAccessProvider, self).__init__()

    def get_fileobj(self, location, manifest_file):
        assert location["provider"]["id"] == "aws-s3-ia"

        bucket = location["bucket"]
        path_prefix = location["path"]

        s3_key = os.path.join(path_prefix, manifest_file["path"])
        s3_obj = self.s3_client.get_object(Bucket=bucket, Key=s3_key)
        return s3_obj["Body"]
