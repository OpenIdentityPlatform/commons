"""
Python lib for deploying, starting and stopping the JASPI runtime
"""
import subprocess
import shutil
import os
import time
import requests
import signal
import glob
from subprocess import STDOUT

class startup_shutdown:

    def __init__(self, lib_directory, tomcat_zip_path, deploy_path, debug):
        """ save the resources directory """
        resources = os.path.join(lib_directory, '..', 'resources')
        self._resources = resources
        # Extract the parent dir as tomcat_deploy_path end with apache-tomcat 
        # and apache-tomcat folder will be created when we unzip
        (head, tail) = os.path.split(tomcat_zip_path)
        # do it again if the tomcat_deploy_path has a trailing backslash
        if tail == "":
            (head, tail) = os.path.split(head)    
        self._tomcat_dir_name = tomcat_dir_name = tail.rsplit('.', 1)[0]   
        self._tomcat_zip_path = tomcat_zip_path
        self._deploy_path = deploy_path
        self._debug = debug

    def clean_deploy_dir(self):
        """ clean deploy directory """
        if os.path.isdir(self._deploy_path):
            shutil.rmtree(self._deploy_path) 

    def deploy_tomcat_python(self, jaspi_test_server_war_path):
        """ deploy JASPI runtime test server """

        # Unzip the file
        if not os.path.exists(self._deploy_path):
            os.makedirs(self._deploy_path)
        os.chdir(self._deploy_path)

        with open(os.devnull, "w") as invisible:
            subprocess.call(["unzip", self._tomcat_zip_path], stdout=invisible)
        print "Successfully deployed tomcat"

        self.set_tomcat_sh_executable()

        shutil.copy(jaspi_test_server_war_path, os.path.join(self._deploy_path, self._tomcat_dir_name, 'webapps'))


    def set_tomcat_sh_executable(self):

        tomcat_deploy_bin_path = os.path.join(self._deploy_path, self._tomcat_dir_name, 'bin', '*.sh')  

        for (sh_path) in glob.glob(tomcat_deploy_bin_path):
            with open(os.devnull, "w") as invisible:
                subprocess.call(["chmod", "u+x", sh_path])


    def startup_tomcat_python(self, port):
        """ start tomcat using bin/catalina.sh script """    

        # First, we will kill any other Tomcat process running on the machine
        # This prevent us from having another Tomcat using another Port
        # but often this is using the same port and break automation
        self.kill_tomcat_processes()
        
        startup_path = os.path.join(self._deploy_path, self._tomcat_dir_name, 'bin', 'catalina.sh')
    
        args = [startup_path]
        if self._debug != 'false':
            args.append('jpda')
        
        args.append('start')

        # Start Tomcat
        p = subprocess.Popen(args, stdout=subprocess.PIPE)

        # startup can take some time before all the bundles are deployed
        url = 'http://localhost:'+str(port)+'/jaspi/status'
        
        while True:
            try:
                result = requests.get(url)
                actual_status = result.status_code
                if actual_status == 200:
                    break
            except:
                pass
            time.sleep(0.5)

     
    def shutdown_tomcat_python(self):
        """ shutdown tomcat using bin/catalina.sh script """
    
        shutdown_path = os.path.join(self._deploy_path, self._tomcat_dir_name, 'bin', 'catalina.sh')
        
        with open(os.devnull, "w") as invisible:
           return_code = subprocess.call([shutdown_path, 'stop'], stdout=invisible)
           if return_code == 0:
               #if shutdown was made, need to wait a bit after shutdown 
               # before doing anything else otherwise get errors 
               time.sleep(0.5) 
            
        
    def kill_tomcat_processes(self):
        """ Kill all the other tomcat processes """
        p = subprocess.Popen(['ps', '-ef'], stdout=subprocess.PIPE)
        out, err = p.communicate()
        for line in out.splitlines():
            if 'tomcat' in line in line:
                pid = int(line.split()[1])
                os.kill(pid, signal.SIGKILL)
        
            

