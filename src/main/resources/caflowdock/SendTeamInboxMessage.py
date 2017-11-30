import flowdock

from caflowdock.FlowdockHelper import FlowdockHelper

api_key = FlowdockHelper.get_token(locals())
fdclient = flowdock.FlowDock(api_key=api_key, app_name=appName, project=project)

fdclient.post(fromAddress, subject, content, from_name = fromName, tags=tags, link=link)
