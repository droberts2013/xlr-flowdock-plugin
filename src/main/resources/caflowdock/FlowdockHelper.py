class FlowdockHelper(object):

    def __init__(self):
        return

    @staticmethod
    def get_token(variables):
        return variables['flowToken'] if variables['flowToken'] else variables['flowdockServer']["flowToken"]
