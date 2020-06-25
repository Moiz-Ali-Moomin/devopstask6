job("website_code_pull") {
description ("Pulling website code")
  scm{
    github('Moiz-Ali-Moomin/websitecode','master')
  }
  triggers {
        githubPush()
    }
}


job("launching_website") {
description ("Launching website on the top of k8s")
  triggers {
        upstream('website_code_pull', 'SUCCESS')
    }
   steps {
        shell('''if ls | grep .html
then 
cd websitecode 
kubectl create -f htmlwebsite.yaml
sleep 5
cd .. 
export html=$(ls | grep .html)
export htmlpod=$(kubectl get deployment | grep htmlwebsite | awk '{print$1}')
sleep 4
kubectl $(echo $html) $(echo $htmlpod):/usr/local/apache2/htdocs/
 
elif ls | grep .php
then 
cd websitecode 
kubectl create -f phpwebsite.yaml
sleep 5
cd .. 
export php=$(ls | grep .php)
export phpod=$(kubectl get deployment | grep phpwebsite | awk '{print$1}') 
sleep 4
kubectl cp $(echo $php) $(echo $phpod):/var/www/html/ 
fi''')
    }
}


job("Relaunching_fail_website") {
description ("It will test if pod is running else send a mail")

  triggers {
        upstream('launching_website', 'SUCCESS')
    }
steps {
        shell('''cp -rvf /root/.jenkins/workspace/job1/* .

export html=$(kubectl get deployment | grep htmlwebsite | awk '{print$1}')
if [[ "$html" == "htmlwebsite"]]
then 
export status=$(curl -sL -w "%{http_code}" -I "http://192.168.99.100:30052" -o /dev/null)
else
export status=$(curl -sL -w "%{http_code}" -I "http://192.168.99.100:30053" -o /dev/null)
fi 

if [[ $status == 200 ]]
then 
echo "website working good"
else 
echo "sending alert email to developer site is not working"
python3 mail.py
sleep 10
if [[ "$html" == "htmlwebsite" ]]
then
kubectl apply -f html_deploy.yaml
else
kubectl apply -f php_deploy.yaml
fi
fi 
''')
    }
}


buildPipelineView('BuildP-Task6') {
    title('Task6-build')
    displayedBuilds(3)
    selectedJob('website_code_pull')
    showPipelineParameters(true)
    refreshFrequency(2)
}