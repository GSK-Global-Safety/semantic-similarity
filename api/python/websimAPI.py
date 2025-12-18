import pandas as pd
import requests
from requests.exceptions import HTTPError

# Set this once for all API functions
API_BASE_URL = "http://localhost:8080/websim/app/template/api,"

#
# Get a list of all SMQs available in the database as a pandas data frame
#
def getAllSmqs():
    # Build the API url to call
    api_url = API_BASE_URL + "SMQ.vm?"
    api_url = api_url + "methodCall=listAll"

    try:
        response = requests.get(api_url)
        response.raise_for_status()
        # Return JSON formatted response
        jsonResponse = response.json()
        #print("Entire JSON response")
        #print(jsonResponse)
        smqs = jsonResponse["smqs"]
        df = pd.DataFrame()
        mySmqs = {}
        for smq in smqs:
            smq_dict = {
                "SMQ_CODE" : smq["code"],
                "SMQ_NAME" : smq["name"],
                "BROAD" : smq["broad"],
                "NARROW" : smq["narrow"]
            }
            mySmqs[smq["code"]] = smq_dict
            
            
        # Convert list of SMQs into a Pandas dataframe
        df = pd.DataFrame.from_dict(mySmqs, orient='index').reset_index(drop=True)
        return (df)
    
    except HTTPError as http_err:
        print(f'HTTP error occurred: {http_err}')
    except Exception as err:
        print(f'Other error occurred: {err}')


#
# Return a data frame with all of the MedDRA codes related to another by HLGT
#
def searchMeddraByCode(pt_code):
    # Build the API url to call
    api_url = API_BASE_URL + "Meddra.vm?"
    api_url = api_url + "methodCall=code_search" + "&"
    api_url = api_url + "pt_code=" + str(pt_code)

    try:
        response = requests.get(api_url)
        response.raise_for_status()
        jsonResponse = response.json()
        code = jsonResponse["code"]
        df = pd.DataFrame()
        myCodes = {}
        myCode = { "PT_CODE" : code['code'],
                   "UMLS_CUI" : code['cui'],
                   "PT_CODE_TTY" : code['tty'],
                   "TERM" : code['term'] }
        myCodes[code['code']] = myCode

        # Convert list of SMQs into a Pandas dataframe
        df = pd.DataFrame.from_dict(myCodes, orient='index').reset_index(drop=True)
        return (df)
        
    except HTTPError as http_err:
        print(f'HTTP error occurred: {http_err}')
    except Exception as err:
        print(f'Other error occurred: {err}')


#
# Return a data frame with all of the MedDRA codes related to another by HLGT
#
def searchMeddraByTerm(term, pt_filter = "ALL", threshold = 0.7):
    # Build the API url to call
    api_url = API_BASE_URL + "Meddra.vm?"
    api_url = api_url + "methodCall=term_search" + "&"
    api_url = api_url + "pt_filter=" + str(pt_filter) + "&"
    api_url = api_url + "probability=" + str(threshold) + "&"
    api_url = api_url + "term=" + str(term)

    try:
        response = requests.get(api_url)
        response.raise_for_status()
        jsonResponse = response.json()
        codes = jsonResponse["codes"]
        df = pd.DataFrame()
        myCodes = {}
        for code in codes:
            myCode = { "PT_CODE" : code['code'],
                       "UMLS_CUI" : code['cui'],
                       "PT_CODE_TTY" : code['tty'],
                       "TERM" : code['term'],
                       "MATCH_PROBABILITY" : code['matchProbability'] }
            myCodes[code['code']] = myCode

        # Convert list of SMQs into a Pandas dataframe
        df = pd.DataFrame.from_dict(myCodes, orient='index').reset_index(drop=True)
        return (df)
        
    except HTTPError as http_err:
        print(f'HTTP error occurred: {http_err}')
    except Exception as err:
        print(f'Other error occurred: {err}')


#
# Return a data frame with all of the MedDRA codes related to another by HLGT
#
def getRelatedHlgtCodes(pt_code, filterFaers = True, pt_filter = "ALL"):
    # Build the API url to call
    api_url = API_BASE_URL + "MeddraHierarchy.vm?"
    api_url = api_url + "methodCall=getRelatedHlgtCodes" + "&"
    api_url = api_url + "faers=" + str(filterFaers) + "&"
    api_url = api_url + "pt_filter=" + str(pt_filter) + "&"
    api_url = api_url + "code=" + str(pt_code)

    try:
        response = requests.get(api_url)
        response.raise_for_status()
        jsonResponse = response.json()
        hltCodes = jsonResponse["codes"]
        df = pd.DataFrame()
        myCodes = {}
        for code in hltCodes:
            myCode = { "PT_CODE" : code['code'],
                       "UMLS_CUI" : code['cui'],
                       "PT_CODE_TTY" : code['tty'],
                       "TERM" : code['term'] }
            myCodes[code['code']] = myCode

        # Convert list of SMQs into a Pandas dataframe
        df = pd.DataFrame.from_dict(myCodes, orient='index').reset_index(drop=True)
        return (df)
        
    except HTTPError as http_err:
        print(f'HTTP error occurred: {http_err}')
    except Exception as err:
        print(f'Other error occurred: {err}')

#
# Return a data frame with all of the MedDRA codes related to another by HLGT
#
def getRelatedHltCodes(pt_code, filterFaers = True, pt_filter = "ALL"):
    # Build the API url to call
    api_url = API_BASE_URL + "MeddraHierarchy.vm?"
    api_url = api_url + "methodCall=getRelatedHltCodes" + "&"
    api_url = api_url + "faers=" + str(filterFaers) + "&"
    api_url = api_url + "pt_filter=" + str(pt_filter) + "&"
    api_url = api_url + "code=" + str(pt_code)

    try:
        response = requests.get(api_url)
        response.raise_for_status()
        jsonResponse = response.json()
        hltCodes = jsonResponse["codes"]
        df = pd.DataFrame()
        myCodes = {}
        for code in hltCodes:
            myCode = { "PT_CODE" : code['code'],
                       "UMLS_CUI" : code['cui'],
                       "PT_CODE_TTY" : code['tty'],
                       "TERM" : code['term'] }
            myCodes[code['code']] = myCode

        # Convert list of SMQs into a Pandas dataframe
        df = pd.DataFrame.from_dict(myCodes, orient='index').reset_index(drop=True)
        return (df)
        
    except HTTPError as http_err:
        print(f'HTTP error occurred: {http_err}')
    except Exception as err:
        print(f'Other error occurred: {err}')

#
# Return a data frame with all of the HLGT's Meddra PTs
#
def getHlgtCodes(hglt_code,  pt_filter = "ALL"):
    # Build the API url to call
    api_url = API_BASE_URL + "MeddraHierarchy.vm?"
    api_url = api_url + "methodCall=getHlgtTerms" + "&"
    api_url = api_url + "pt_filter=" + str(pt_filter) + "&"
    api_url = api_url + "hlgt=" + str(hglt_code)

    try:
        response = requests.get(api_url)
        response.raise_for_status()
        # Return JSON formatted response
        jsonResponse = response.json()
        #print("Entire JSON response")
        #print(jsonResponse)
        hlgtCodes = jsonResponse["hlgt"]
        df = pd.json_normalize(hlgtCodes, record_path = ["codes"])
        new_cols = { "code" : "PT_CODE",
                     "cui"  : "UMLS_CUI",
                     "tty"  : "PT_CODE_TTY",
                     "term" : "TERM", }
        # Rename, then reorder the columns
        df = df.rename( columns = new_cols )
        return (df)
    
    except HTTPError as http_err:
        print(f'HTTP error occurred: {http_err}')
    except Exception as err:
        print(f'Other error occurred: {err}')

#
# Return a data frame with all of the SMQ's Meddra PTs
# @param: code = SMQ code
# @param: scope = If narrow, will only return narrow, otherwise broad
# @param: pt_filter = Options PT_ONLY (default) or ALL, if PT_ONLY, will only return PT terms
#
def getSmqCodes(code, scope,  pt_filter = "ALL"):
    # Build the API url to call
    api_url = API_BASE_URL + "SMQ.vm?"
    api_url = api_url + "methodCall=getSMQ" + "&"
    api_url = api_url + "smq=" + str(code) + "&"
    api_url = api_url + "scope=" + str(scope) + "&"
    api_url = api_url + "pt_filter=" + str(pt_filter)

    try:
        response = requests.get(api_url)
        response.raise_for_status()
        # Return JSON formatted response
        jsonResponse = response.json()
        #print("Entire JSON response")
        #print(jsonResponse)
        smqCodes = jsonResponse["smq"]
        df = pd.json_normalize(smqCodes, record_path = ["codes"])

        new_cols = { "code" : "PT_CODE",
                     "cui"  : "UMLS_CUI",
                     "tty"  : "PT_CODE_TTY",
                     "term" : "TERM", }
        # Rename, then reorder the columns
        df = df.rename( columns = new_cols )
        
        return (df)
    
    except HTTPError as http_err:
        print(f'HTTP error occurred: {http_err}')
    except Exception as err:
        print(f'Other error occurred: {err}')

#
# Return a data frame with all of the codes found in an SMQ related to this code
# @param: pt_code = MedDRA PT code to search for in SMQs
# @param: scope = If narrow, will only return narrow, otherwise broad
# @param: pt_filter = Options PT_ONLY (default) or ALL, if PT_ONLY, will only return PT terms
#
def getCodesRelatedBySmq(pt_code, scope,  pt_filter = "ALL"):
    # Build the API url to call
    api_url = API_BASE_URL + "SMQ.vm?"
    api_url = api_url + "methodCall=getRelatedSMQ" + "&"
    api_url = api_url + "pt_code=" + str(pt_code) + "&"
    api_url = api_url + "scope=" + str(scope) + "&"
    api_url = api_url + "pt_filter=" + str(pt_filter)

    try:
        response = requests.get(api_url)
        response.raise_for_status()
        # Return JSON formatted response
        jsonResponse = response.json()
        smqCodes = jsonResponse["codes"]
        df = pd.DataFrame()
        myCodes = {}
        for code in smqCodes:
            myCode = { "PT_CODE" : code['code'],
                       "UMLS_CUI" : code['cui'],
                       "PT_CODE_TTY" : code['tty'],
                       "TERM" : code['term'] }
            myCodes[code['code']] = myCode

        # Convert list of codes into a Pandas dataframe
        df = pd.DataFrame.from_dict(myCodes, orient='index').reset_index(drop=True)
        return (df)
    
    except HTTPError as http_err:
        print(f'HTTP error occurred: {http_err}')
    except Exception as err:
        print(f'Other error occurred: {err}')


def convertMatrixToDataFrame(matrix, metrics, keepMax):
    metric_array = metrics.split(",")
    df = pd.json_normalize(matrix, record_path = ["distances"]) #(columns = ["PT_CODE_1", "PT_CODE_2", metric_array])
    #print(df.columns)
    # Rename columns
    new_cols = { "code1.code" : "PT_CODE_1",
                 "code1.tty"  : "PT_CODE_1_TTY",
                 "code1.term" : "TERM_1",
                 "code2.code" : "PT_CODE_2",
                 "code2.tty"  : "PT_CODE_2_TTY",
                 "code2.term" : "TERM_2",
                 "maxMetric"  : "MAX_METRIC" }
    for metric in metrics.split(","):
        new_cols[ "distanceMetrics." + metric ] = metric
    # Rename, then reorder the columns
    df = df.rename( columns = new_cols )

    # Drop the columns except for these
    cols_to_keep = ["PT_CODE_1", "TERM_1", "PT_CODE_2", "TERM_2" ]
    for metric in metrics.split(","):
        cols_to_keep.append(metric)

    # This only applies to the inner matrix
    if ( keepMax ):
        cols_to_keep.append("MAX_METRIC")
    
    df = df[cols_to_keep]
    return(df)

 


# Find codes related to a single code
# This supports only calling one ontology view at a time
#
# @param: view = ontology view (e.g. mdr-umls, sct-mdr)
# @param: metrics = Comma separated list of distance metrics to compute
# @param: pt_code = PT Code to find related codes for
# @param: filterFaers = set to 'true' to limit computation to PTs which exist in FAERS (2022-Q4)
# @param: pt_filter = Options PT_ONLY (default) or ALL, if PT_ONLY, will only return PT terms
# @param: minDistance = if set, then will only return matrix entries >= to minDistance filter
#
def getRelatedCodeMatrix(view, metrics, pt_code, filterFaers = True, pt_filter = "PT_ONLY", minDistance = None):
    # Build the API url to call
    api_url = API_BASE_URL + "MeddraHierarchy.vm?"
    api_url = api_url + "methodCall=getRelatedCodes" + "&"
    api_url = api_url + "ontology=" + str(view) + "&"
    api_url = api_url + "metrics=" + str(metrics) + "&"
    api_url = api_url + "code=" + str(pt_code) + "&"
    api_url = api_url + "faers=" + str(filterFaers) + "&"
    api_url = api_url + "pt_filter=" + str(pt_filter)
    if minDistance != None:
        api_url = api_url + "&minDistance=" + str(minDistance)

    try:
        response = requests.get(api_url)
        response.raise_for_status()
        # Return JSON formatted response
        jsonResponse = response.json()

        # If you wanted to get code term and term type, you could extract those from the JSON object
        #print("Entire JSON response")
        #print(jsonResponse)
        matrix = jsonResponse["matrix"]
        return convertMatrixToDataFrame(matrix, metrics, False)
    except HTTPError as http_err:
        print(f'HTTP error occurred: {http_err}')
    except Exception as err:
        print(f'Other error occurred: {err}')      


#
# Get a distance matrix API call
#
# @param: view = ontology view (e.g. mdr-umls, sct-mdr)
# @param: metrics = Comma separated list of distance metrics to compute
# @param: smq = SMQ Code to compute matrix for
# @param: scope = set to 'narrow' to only use the narrow SMQ definition, any other value will return broad+narrow
# @param: innerOuter = set to 'inner' to compute distance matrix on inner PT codes only
# @param: filterFaers = set to 'true' to limit computation to PTs which exist in FAERS (2022-Q4)
# @param: pt_filter = PT_ONLY will return only PT codes, set to ALL for LLT inclusion
# @param: minDistance = if set, then will only return matrix entries >= to minDistance filter
#
def getDistanceMatrix(view, metrics, smq, scope, innerOuter, filterFaers = False, pt_filter = "PT_ONLY", minDistance = None):
    # Build the API url to call
    api_url = API_BASE_URL + "SMQMatrix.vm?"
    api_url = api_url + "methodCall=computeDistanceMatrix" + "&"
    api_url = api_url + "ontology=" + str(view) + "&"
    api_url = api_url + "metrics=" + str(metrics) + "&"
    api_url = api_url + "smq=" + str(smq) + "&"
    api_url = api_url + "scope=" + str(scope) + "&"
    api_url = api_url + "inner=" + str(innerOuter) + "&"
    api_url = api_url + "pt_filter=" + str(pt_filter) + "&"
    api_url = api_url + "faers=" + str(filterFaers)
    if minDistance != None:
        api_url = api_url + "&minDistance=" + str(minDistance)

    try:
        response = requests.get(api_url)
        response.raise_for_status()
        # Return JSON formatted response
        jsonResponse = response.json()

        # If you wanted to get code term and term type, you could extract those from the JSON object
        #print("Entire JSON response")
        #print(jsonResponse)
        matrix = jsonResponse["matrix"]
        #print(matrix)
        #print("Converting to dataframe")
        if innerOuter == "inner":
            return convertMatrixToDataFrame(matrix, metrics, True)
        else:
            return convertMatrixToDataFrame(matrix, metrics, False)
    except HTTPError as http_err:
        print(f'HTTP error occurred: {http_err}')
    except Exception as err:
        print(f'Other error occurred: {err}')

#
# Get a distance matrix API call
#
# @param: view = ontology view (e.g. mdr-umls, sct-mdr)
# @param: metrics = Comma separated list of distance metrics to compute
# @param: pt_codes = List of PT codes to compute matrix
# @param: filterFaers = If true, will only return PT or LLT codes found in Faers
# @param: pt_filter = PT_ONLY will return only PT codes, set to ALL for LLT inclusion
# @param: minDistance = if set, then will only return matrix entries >= to minDistance filter
#
def getPTMatrix(view, metrics, pt_codes, filterFaers = False, pt_filter = "PT_ONLY", minDistance = None):
    # Build the API url to call
    api_url = API_BASE_URL + "ComputeMatrix.vm?"
    api_url = api_url + "methodCall=ptMatrix" + "&"
    api_url = api_url + "ontology=" + str(view) + "&"
    api_url = api_url + "metrics=" + str(metrics) + "&"
    api_url = api_url + "faers=" + str(filterFaers) + "&"
    api_url = api_url + "pt_filter=" + str(pt_filter) + "&"    
    api_url = api_url + "pt_codes=" + str(pt_codes)
    if minDistance != None:
        api_url = api_url + "&minDistance=" + str(minDistance)
    try:
        response = requests.get(api_url)
        response.raise_for_status()
        # Return JSON formatted response
        jsonResponse = response.json()
        matrix = jsonResponse["matrix"]
        return convertMatrixToDataFrame(matrix, metrics, True)
    except HTTPError as http_err:
        print(f'HTTP error occurred: {http_err}')
    except Exception as err:
        print(f'Other error occurred: {err}')

#
# Get a distance matrix API call
#
# @param: view = ontology view (e.g. mdr-umls, sct-mdr)
# @param: metrics = Comma separated list of distance metrics to compute
# @param: hlgt_code = HLGT Code
# @param: filterFaers = If true, will only return PT or LLT codes found in Faers
# @param: pt_filter = PT_ONLY will return only PT codes, set to ALL for LLT inclusion
# @param: minDistance = if set, then will only return matrix entries >= to minDistance filter
#
def getHLGTMatrix(view, metrics, hlgt_code, filterFaers = False, pt_filter = "PT_ONLY", minDistance = None):
    # Build the API url to call
    api_url = API_BASE_URL + "ComputeMatrix.vm?"
    api_url = api_url + "methodCall=hlgtMatrix" + "&"
    api_url = api_url + "ontology=" + str(view) + "&"
    api_url = api_url + "metrics=" + str(metrics) + "&"
    api_url = api_url + "faers=" + str(filterFaers) + "&"
    api_url = api_url + "hlgt_code=" + str(hlgt_code) + "&"
    api_url = api_url + "pt_filter=" + str(pt_filter)
    if minDistance != None:
        api_url = api_url + "&minDistance=" + str(minDistance)
    try:
        response = requests.get(api_url)
        response.raise_for_status()
        # Return JSON formatted response
        jsonResponse = response.json()
        matrix = jsonResponse["matrix"]
        return convertMatrixToDataFrame(matrix, metrics, True)
    except HTTPError as http_err:
        print(f'HTTP error occurred: {http_err}')
    except Exception as err:
        print(f'Other error occurred: {err}')
        
# Test API call
def getDistance(view, metric, code1, code2):

    # Build the API url to call
    api_url = API_BASE_URL + "GetDistance.vm?"
    api_url = api_url + "onto=" + str(view) + "&"
    api_url = api_url + "metric=" + str(metric) + "&"
    api_url = api_url + "code1=" + str(code1) + "&"
    api_url = api_url + "code2=" + str(code2)
    try:
        response = requests.get(api_url)
        response.raise_for_status()
        # Return JSON formatted response
        jsonResponse = response.json()

        # Test for errors
        if "metrics" in jsonResponse.keys():
            # We really only need the distance returned from the function call
            distance = jsonResponse["metrics"]["distance"]
            return distance
        else:            
            print("Invalid or missing code provided")
            return None

        # If you wanted to get code term and term type, you could extract those from the JSON object
        #print("Entire JSON response")
        #print(jsonResponse)

    except HTTPError as http_err:
        print(f'HTTP error occurred: {http_err}')
    except Exception as err:
        print(f'Other error occurred: {err}')


#
# @param: smq = SMQ Code to compute clusters
# @param: scope = set to 'narrow' to only use the narrow SMQ definition, any other value will return broad+narrow
# @param: metrics = Comma separated list of distance metrics to compute
# @param: views = Commas seperated list of ontology views (e.g. mdr-umls, sct-mdr)
#
def getCluster(smq, scope, metrics, views):
    # Build the API url to call
    api_url = API_BASE_URL + "SMQCluster.vm?"
    api_url = api_url + "ontologies=" + str(views) + "&"
    api_url = api_url + "metrics=" + str(metrics) + "&"
    api_url = api_url + "smq=" + str(smq) + "&"
    api_url = api_url + "scope=" + str(scope) 
    try:
        response = requests.get(api_url)
        response.raise_for_status()
        # Return JSON formatted response
        jsonResponse = response.json()
        #print(jsonResponse)
        
        #clusters = jsonResponse["clusters"]
        df = pd.json_normalize(jsonResponse, record_path = ["clusters"])
        
        # Column remap for data frame labels
        new_cols = { "clusterId" : "CLUSTER",
                     "metric"  : "SSM",
                     "level"  : "SSM_VALUE",
                     "ontology" : "ONTOLOGY", 
                     "ptCode" : "PT_CODE_1", 
                     "tty" : "TTY", 
                     "ptTerm" : "PT_TERM" }
                   
        # Rename, then reorder the columns
        df = df.rename( columns = new_cols )
        df['SMQ'] = smq
        cols_to_keep = [ "SMQ", "ONTOLOGY", "SSM", "SSM_VALUE", "CLUSTER", "PT_CODE_1" ]
        df = df[cols_to_keep]
        return (df)
    except HTTPError as http_err:
        print(f'HTTP error occurred: {http_err}')
    except Exception as err:
        print(f'Other error occurred: {err}')

