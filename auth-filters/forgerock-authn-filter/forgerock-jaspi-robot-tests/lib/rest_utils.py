"""
Python Lib for REST access for OpenIDM

By default we use Content-Type: application/json
It assumes everything, server and testclient, is UTF-8
if they have different configuration (Server running on ASCII or ISO something)
and test fails because the conversion 
then we could try to execute the same call with Content-Type: application/json;charset=utf-8 
which would lead to a success request

"""
import requests
from robot.reporting import resultwriter

class rest_utils:
    """ Python Lib for REST access """  
    

    def __init__(self, url_prefix):
        """ save the URL prefix in the object """
        self._prefix = url_prefix
    
        
#########################################################################
# LOW LEVEL KEYWORDS: call Requests Library Methods
# Created so that we don't have to name the argument within our TC
######################################################################### 
            
    def get(self, url, headers=None):
        """ direct call to Requests lib """  
        if headers is None:
            headers = {}
        result = requests.get(url, headers=headers)
        return result    
    
    def delete(self, url, headers=None):
        """ direct call to Requests lib """  
        if headers is None:
            headers = {}
        result = requests.delete(url, headers=headers)
        return result    

    def head(self, url, headers=None):
        """ direct call to Requests lib """  
        if headers is None:
            headers = {}
        result = requests.head(url, headers=headers)
        return result        
    
    def post(self, url, data_string, headers=None):
        """ direct call to Requests lib """  
        if headers is None:
            headers = {}
        result = requests.post(url, data=data_string, headers=headers)
        return result 
    
    def put(self, url, data_string, headers=None):
        """ direct call to Requests lib """  
        if headers is None:
            headers = {}
        result = requests.put(url, data=data_string, headers=headers)
        return result     
    
    def patch(self, url, data_string, headers=None):
        """ direct call to Requests lib """  
        if headers is None:
            headers = {}
        result = requests.patch(url, data=data_string, headers=headers)
        return result     




    def put_url(self, url_endpoint, data_string, headers_custom=None):
        """
        As Admin, perform a put, check returned status and return JSON
        """
        if headers_custom is None:
            headers_custom = {}
        headers_content = {'Content-Type':'application/json'}
        url = self._prefix + url_endpoint
        headers = dict(headers_content.items() + headers_custom.items())
        result = requests.put(url, data=data_string, headers=headers)
        if result.status_code != 200 and result.status_code != 201:
            msg = "Status code expected: 200 or 201 but found: "+str(result.status_code)
            raise Exception(msg)
        return result.json()