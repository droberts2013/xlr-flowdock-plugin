import os
import tempfile
from com.xebialabs.xlr.ssl import LoaderUtil
from java.nio.file import Files, Paths, StandardCopyOption

def set_ca_bundle_path():
    ca_bundle_path = extract_file_from_jar("requests/cacert.pem")
    os.environ['REQUESTS_CA_BUNDLE'] = ca_bundle_path


def extract_file_from_jar(config_file):
    file_url = LoaderUtil.getResourceBySelfClassLoader(config_file)
    if file_url:
        tmp_file, tmp_abs_path = tempfile.mkstemp()
        tmp_file.close()
        Files.copy(file_url.openStream(), Paths.get(tmp_abs_path), StandardCopyOption.REPLACE_EXISTING)
        return tmp_abs_path
    else:
        return None

if 'REQUESTS_CA_BUNDLE' not in os.environ:
    set_ca_bundle_path()
