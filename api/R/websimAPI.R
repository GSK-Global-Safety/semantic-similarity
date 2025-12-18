library(httr)
library(jsonlite)
library(dplyr)
library(magrittr)
library(tidyr)

# This is required to create a local cache of 
# results returned from the API call to prevent repeats
library(digest)

# For parallel safe calling
library(parallel)

# Initialize cache environment
WEBSIM_API_CACHE <- new.env(parent = emptyenv())

# Function to generate a cache key based on parameters
generateCacheKey <- function(view, metrics, pt_code, filterFaers, pt_filter, minDistance) {
  key <- paste(view, metrics, pt_code, filterFaers, pt_filter, minDistance, sep = "_")
  return(digest(key, algo = "md5"))
}


#
# Once you have your websim API server up and running
# update the URL to point to it here.
# Set this once for all API functions
#
API_BASE_URL <- "http://localhost:8080/websim/app/template/api,"


perform_api_call <- function(api_url, max_retries = 5, base_delay = 1) {
  for (attempt in 1:max_retries) {
    response <- tryCatch({
      GET(api_url)
    }, error = function(e) {
      message("Error occurred: ", e$message)
      return(NULL)
    })
    
    if (!is.null(response) && response$status_code == 200) {
      return(response)
    }
    
    delay <- base_delay * 2^(attempt - 1)
    Sys.sleep(delay)
  }
  message("Failed to get a valid response after ", max_retries, " attempts.")
  return(NULL)
}


# Get the distance between two meddra codes
getDistance <- function(view, metric, code1, code2) {
  # Build the API url to call
  api_url <- paste0(API_BASE_URL, "GetDistance.vm?",
                    "onto=", view, "&",
                    "metric=", metric, "&",
                    "code1=", code1, "&",
                    "code2=", code2)
  #print(api_url)
  
  # Perform the GET request
  response <- perform_api_call(api_url)
  if (is.null(response)) {
    return(NULL)
  }

  # Check if the response status is OK
  if (response$status_code != 200) {
    message("HTTP error occurred: ", response$status_code)
    return(NULL)
  }
  
  # Parse the JSON response
  jsonResponse <- content(response, as = "text", encoding = "UTF-8")
  jsonResponse <- fromJSON(jsonResponse)
  
  # Test for errors
  if ("metrics" %in% names(jsonResponse)) {
    # We really only need the distance returned from the function call
    distance <- jsonResponse$metrics$distance
    return(distance)
  } else {
    message("Invalid or missing code provided")
    return(NULL)
  }
}



# Get a list of all SMQs available in the database as an R data frame
getAllSmqs <- function() {
  
  # Build the API url to call
  api_url <- paste0(API_BASE_URL, "SMQ.vm?methodCall=listAll")
  
  # Perform the GET request
  response <- perform_api_call(api_url)
  if (is.null(response)) {
    return(NULL)
  }

  # Check if the response status is OK
  if (response$status_code != 200) {
    message("HTTP error occurred: ", response$status_code)
    return(NULL)
  }
  
  # Parse the JSON response
  jsonResponse <- content(response, as = "text", encoding = "UTF-8")
  jsonResponse <- fromJSON(jsonResponse, flatten = TRUE)
  
  # Extract the SMQs
  smqs <- jsonResponse$smqs
  
  # Initialize an empty list to store SMQs
  mySmqs <- list()
  
  # Iterate over the SMQs and extract relevant fields
  for (i in seq(1, length(smqs), by=5)) {
    smq_dict <- data.frame(
      SMQ_CODE = smqs[["code"]],
      SMQ_NAME = smqs[["name"]],
      BROAD = smqs[["broad"]],
      NARROW = smqs[["narrow"]],
      stringsAsFactors = FALSE
    )
    mySmqs <- append(mySmqs, list(smq_dict))
  }
  
  # Combine all data frames into one
  if (length(mySmqs) > 0) {
    df <- do.call(rbind, mySmqs)
    rownames(df) <- NULL
    return(df)
  } else {
    message("No valid SMQ data found.")
    return(data.frame())
  }
}


#
# Return a data frame with all of the SMQ's Meddra PTs
# @param: code = SMQ code
# @param: scope = If narrow, will only return narrow, otherwise broad
# @param: pt_filter = Options PT_ONLY (default) or ALL, if PT_ONLY, will only return PT terms
#

getSmqCodes <- function(code, scope, pt_filter = "ALL") {
  API_BASE_URL <- "http://localhost:8080/websim/app/template/api,"
  
  # Build the API url to call
  api_url <- paste0(API_BASE_URL, "SMQ.vm?methodCall=getSMQ&smq=", code, "&scope=", scope, "&pt_filter=", pt_filter)
  
  # Perform the GET request
  response <- perform_api_call(api_url)
  if (is.null(response)) {
    return(NULL)
  }
  

  # Check if the response status is OK
  if (response$status_code != 200) {
    message("HTTP error occurred: ", response$status_code)
    return(NULL)
  }
  
  # Parse the JSON response
  jsonResponse <- content(response, as = "text", encoding = "UTF-8")
  jsonResponse <- fromJSON(jsonResponse, flatten = TRUE)
  
  # Extract the SMQ codes
  smqCodes <- jsonResponse$smq$codes
  
  # Convert to a data frame
  df <- as.data.frame(smqCodes)
  
  # Rename columns
  new_cols <- c("code" = "PT_CODE",
                "cui" = "UMLS_CUI",
                "tty" = "PT_CODE_TTY",
                "term" = "TERM")
  
  if (ncol(df) > 0) {
    df <- df %>% rename_with(~ new_cols[.x], all_of(names(new_cols)))
  }
  
  return(df)
}







# Return the PT code in a data frame with it's CUI, Term and Term Type
searchMeddraByCode <- function(pt_code) {
  # Build the API url to call
  api_url <- paste0(API_BASE_URL, "Meddra.vm?methodCall=code_search&pt_code=", pt_code)
  
  # Perform the GET request
  response <- perform_api_call(api_url)
  if (is.null(response)) {
    return(NULL)
  }
  
  # Check if the response status is OK
  if (response$status_code != 200) {
    message("HTTP error occurred: ", response$status_code)
    return(NULL)
  }
  
  # Parse the JSON response
  jsonResponse <- content(response, as = "text", encoding = "UTF-8")
  jsonResponse <- fromJSON(jsonResponse, flatten = TRUE)
  
  # Extract the code information
  code <- jsonResponse$code
  
  # Create a data frame with the code information
  df <- data.frame(
    PT_CODE = code$code,
    UMLS_CUI = code$cui,
    PT_CODE_TTY = code$tty,
    TERM = code$term,
    stringsAsFactors = FALSE
  )
  
  return(df)
}



# Return the PT code using the term as a search criteria
searchMeddraByTerm <- function(term, pt_filter = "ALL", threshold = 0.7) {
  # Build the API url to call
  api_url <- paste0(API_BASE_URL, "Meddra.vm?methodCall=term_search&pt_filter=", pt_filter, "&probability=", threshold, "&term=", URLencode(term, reserved = TRUE))
  #print(api_url)
  
  # Perform the GET request
  response <- perform_api_call(api_url)
  if (is.null(response)) {
    return(NULL)
  }
  
  # Check if the response status is OK
  if (response$status_code != 200) {
    message("HTTP error occurred: ", response$status_code)
    return(NULL)
  }
  
  # Parse the JSON response
  jsonResponse <- content(response, as = "text", encoding = "UTF-8")
  jsonResponse <- fromJSON(jsonResponse, flatten = TRUE)
  
  # Extract the codes information
  codes <- jsonResponse$codes
  
  # Check if the codes data frame exists and has the expected structure
  if (is.data.frame(codes) && all(c("code", "cui", "aui", "tty", "term", "matchProbability") %in% names(codes))) {
    # Rename the columns to match the desired format
    df <- codes
    colnames(df) <- c("MATCH_PROBABILITY", "PT_CODE", "AUI", "UMLS_CUI", "PT_CODE_TTY", "TERM")
    return(df)
  } else {
    message("No valid codes data found.")
    return(data.frame())
  }
}



# Function to convert matrix to data frame
convertMatrixToDataFrame <- function(matrix, metrics, keepMax) {
  metric_array <- unlist(strsplit(metrics, ","))
  
  # Extract distances from the matrix
  distances <- matrix$distances
  
  # Check if distances is NULL or has no rows
  if (is.null(distances) || length(distances) == 0 || nrow(distances) == 0) {
    # Create an empty data frame with the appropriate column names
    cols_to_keep <- c("PT_CODE_1", "TERM_1", "PT_CODE_2", "TERM_2", metric_array)
    if (keepMax) {
      cols_to_keep <- c(cols_to_keep, "MAX_METRIC")
    }
    df <- data.frame(matrix(ncol = length(cols_to_keep), nrow = 0))
    colnames(df) <- cols_to_keep
    return(df)
  }
  
  # Initialize an empty list to store rows
  rows <- list()
  
  for (distance in 1:nrow(distances)) {
    row <- distances[distance, ]
    
    row_data <- data.frame(
      PT_CODE_1 = row[["code1.code"]],
      TERM_1 = row[["code1.term"]],
      PT_CODE_2 = row[["code2.code"]],
      TERM_2 = row[["code2.term"]],
      stringsAsFactors = FALSE
    )
    
    for (metric in metric_array) {
      row_data[[metric]] <- row[[paste0("distanceMetrics.", metric)]]
    }
    
    if (keepMax) {
      row_data[["MAX_METRIC"]] <- row[["maxMetric"]]
    }
    
    rows <- append(rows, list(row_data))
  }
  
  # Combine all rows into a single data frame
  df <- do.call(rbind, rows)
  
  # Columns to keep
  cols_to_keep <- c("PT_CODE_1", "TERM_1", "PT_CODE_2", "TERM_2", metric_array)
  
  if (keepMax) {
    cols_to_keep <- c(cols_to_keep, "MAX_METRIC")
  }
  
  if (nrow(df) > 0) {
    df <- df[, cols_to_keep, drop = FALSE]
  }
  
  return(df)
}


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
getRelatedCodeMatrix <- function(view, metrics, pt_code, filterFaers = TRUE, pt_filter = "PT_ONLY", minDistances = NULL) {
  
  metric_array <- unlist(strsplit(metrics, ","))
  
  # Check if minDistances is provided and its length matches the number of metrics
  if (!is.null(minDistances) && length(minDistances) != length(metric_array)) {
    stop("The length of minDistances must match the number of metrics.")
  }
  
  # Convert minDistances to a comma-separated string
  minDistances_str <- paste(minDistances, collapse = ",")
  
  # Build the API url to call
  api_url <- paste0(API_BASE_URL, 
                    "MeddraHierarchy.vm?methodCall=getRelatedCodes&ontology=", view,
                    "&metrics=", metrics, 
                    "&code=", pt_code, 
                    "&faers=", filterFaers, 
                    "&pt_filter=", pt_filter)
  if (!is.null(minDistances)) {
    api_url <- paste0(api_url, "&minDistance=", minDistances_str)
  }
  
  # Generate cache key
  cache_key <- generateCacheKey(view, metrics, pt_code, filterFaers, pt_filter, minDistances)
  
  # Check if result is in cache
  if (exists(cache_key, envir = WEBSIM_API_CACHE)) {
    message("Returning cached result")
    return(get(cache_key, envir = WEBSIM_API_CACHE))
  }  
  
  # Perform the GET request
  response <- perform_api_call(api_url)
  if (is.null(response)) {
    return(NULL)
  }
  
  # Check if the response status is OK
  if (response$status_code != 200) {
    message("HTTP error occurred: ", response$status_code)
    return(NULL)
  }
  
  # Parse the JSON response
  jsonResponse <- content(response, as = "text", encoding = "UTF-8")
  jsonResponse <- fromJSON(jsonResponse, flatten = TRUE)
  
  # Extract the matrix
  matrix <- jsonResponse$matrix
  
  # Convert the matrix to a data frame
  df <- convertMatrixToDataFrame(matrix, metrics, FALSE)
  
  # Store the result in cache
  assign(cache_key, df, envir = WEBSIM_API_CACHE)
  return(df)
}




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
getNormalizedRelatedCodeMatrix <- function(view, metrics, pt_code, filterFaers = TRUE, pt_filter = "PT_ONLY", minDistances = NULL, minRange = 0.0, maxRange = 1.0) {
  
  metric_array <- unlist(strsplit(metrics, ","))
  
  # Check if minDistances is provided and its length matches the number of metrics
  if (!is.null(minDistances) && length(minDistances) != length(metric_array)) {
    stop("The length of minDistances must match the number of metrics.")
  }
  
  # Convert minDistances to a comma-separated string
  minDistances_str <- paste(minDistances, collapse = ",")
  
  # Build the API url to call
  api_url <- paste0(API_BASE_URL, 
                    "MeddraHierarchy.vm?methodCall=getNormalizedRelatedCodes&ontology=", view,
                    "&metrics=", metrics, 
                    "&code=", pt_code, 
                    "&faers=", filterFaers, 
                    "&minRange=", minRange, 
                    "&maxRange=", maxRange, 
                    "&pt_filter=", pt_filter)
  if (!is.null(minDistances)) {
    api_url <- paste0(api_url, "&minDistance=", minDistances_str)
  }
  
  # Generate cache key
  cache_key <- generateCacheKey(view, metrics, pt_code, filterFaers, pt_filter, minDistances)
  
  # Check if result is in cache
  if (exists(cache_key, envir = WEBSIM_API_CACHE)) {
    message("Returning cached result")
    return(get(cache_key, envir = WEBSIM_API_CACHE))
  }  
  
  #print(api_url)
  
  # Perform the GET request
  response <- perform_api_call(api_url)
  if (is.null(response)) {
    return(NULL)
  }
  
  # Check if the response status is OK
  if (response$status_code != 200) {
    message("HTTP error occurred: ", response$status_code)
    return(NULL)
  }
  
  # Parse the JSON response
  jsonResponse <- content(response, as = "text", encoding = "UTF-8")
  jsonResponse <- fromJSON(jsonResponse, flatten = TRUE)
  
  # Extract the matrix
  matrix <- jsonResponse$matrix
  
  # Convert the matrix to a data frame
  df <- convertMatrixToDataFrame(matrix, metrics, FALSE)
  
  # Store the result in cache
  assign(cache_key, df, envir = WEBSIM_API_CACHE)
  return(df)
}




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
getPTMatrix <- function(view, metrics, pt_codes, filterFaers = FALSE, pt_filter = "PT_ONLY", minDistance = NULL) {
  
  
  # Check if minDistance is provided and its length matches the number of metrics
  metric_array <- unlist(strsplit(metrics, ","))
  if (!is.null(minDistance) && length(minDistance) != length(metric_array)) {
    stop("The length of minDistance must match the number of metrics.")
  }
  
  # Convert minDistance to a comma-separated string
  if (!is.null(minDistance)) {
    minDistance_str <- paste(minDistance, collapse = ",")
  }
  
  # Build the API url to call
  api_url <- paste0(API_BASE_URL, "ComputeMatrix.vm?methodCall=ptMatrix&ontology=", view,
                    "&metrics=", metrics, "&faers=", tolower(as.character(filterFaers)), "&pt_filter=", pt_filter,
                    "&pt_codes=", paste(pt_codes, collapse = ","))
  
  if (!is.null(minDistance)) {
    api_url <- paste0(api_url, "&minDistance=", minDistance_str)
  }
  
  # Perform the GET request
  response <- perform_api_call(api_url)
  if (is.null(response)) {
    return(NULL)
  }
  
  # Check if the response status is OK
  if (response$status_code != 200) {
    message("HTTP error occurred: ", response$status_code)
    return(NULL)
  }
  
  # Parse the JSON response
  jsonResponse <- content(response, as = "text", encoding = "UTF-8")
  jsonResponse <- fromJSON(jsonResponse, flatten = TRUE)
  
  # Extract the matrix
  matrix <- jsonResponse$matrix
  
  # Convert the matrix to a data frame
  df <- convertMatrixToDataFrame(matrix, metrics, TRUE)
  
  return(df)
}

