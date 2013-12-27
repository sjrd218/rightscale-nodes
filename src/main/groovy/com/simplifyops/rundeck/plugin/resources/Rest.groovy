package com.simplifyops.rundeck.plugin.resources

import com.sun.jersey.api.client.*
import com.sun.jersey.api.client.config.ClientConfig

import javax.ws.rs.core.MultivaluedMap
import com.sun.jersey.core.util.MultivaluedMapImpl
import javax.ws.rs.core.MediaType
import com.sun.jersey.api.representation.Form
import com.sun.jersey.api.client.filter.LoggingFilter
import groovy.xml.MarkupBuilder

/**
 * Simplified groovy REST utility using Jersey client library.
 * <p>
 * You can configure static defaults for use with all instances of this class.
 * </p>
 * <p>Static configuration:</p>
 * <pre>
 * //set Accept header default value
 * Rest.defaultAccept='application/*+xml; version=1.5'
 * //set other default request headers
 * Rest.defaultHeaders=['Authorization':'Basic '+"${user}:${pass}".toString().bytes.encodeBase64().toString()]
 * //set handler for non 200 responses
 * Rest.failureHandler={response->
 *   die("Request failed: ${response}")
 * }
 * //set handler for unexpected content types, used with response.requireContentType(type)
 * Rest.contentTypeFailureHandler={response->
 *   die("ContentType not expected: ${response.type}: ${response}")
 * }
 * </pre>
 * <p>Non-static usage is performed by creating an instance with an absolute or relative URL</p>
 * <pre>
 * def rest = new Rest("http://host/path")
 * def response= rest.post("content",[header:'value'],[query:'value'])
 * response= rest.get([header:'value'],[query:'value'])
 * // use a sub path on the original
 * def resp2 = rest.path("/sub").get()
 * // require content type. If it is not correct, the contentTypeFailureHandler will be called if set
 * resp2.requireContentType("application/xml")
 * </pre>
 * <p>Shortcuts:</p>
 * <pre>
 * //convert String to Rest object:
 * def rest = new Rest("http://host/path")
 *
 * //add path to existing Rest object:
 * def rest2 = rest + '/subpath'
 *
 * //POST xml content using leftshift and groovy markupbuilder
 * def resp = rest2 &lt;&lt; {
 * 	 content(attr:'value'){
 *      sub('text')
 *	 }	
 * }
 *
 * //GET subpath
 * def resp2 = rest['/subpath']
 *
 * //PUT xml at subpath
 * def resp3 = rest['/subpath']={
 *     element('text')
 * }
 *
 * //convert XML response into groovy nodes via XmlParser using .XML
 * def xml = rest['/path'].XML
 *
 * //get textual response using the 'text' property of the response
 * def text = rest['/path'].text
 * </pre>
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class Rest{

	static{
		ClientResponse.metaClass.getXML={
			new XmlParser(false,true).parse(delegate.getEntity(InputStream.class))
		}
		ClientResponse.metaClass.getText={
			delegate.getEntity(String.class)
		}
		ClientResponse.metaClass.requireContentType={type->
			if(!delegate.hasContentType(type)){
				if(Rest.contentTypeFailureHandler) Rest.contentTypeFailureHandler.call(type,delegate)
				else throw new RuntimeException("Expected ${type}, but response was ${delegate.type}: ${delegate}")
			}
			delegate
		}
		ClientResponse.metaClass.requireCompatibleType={type->
			if(!delegate.hasCompatibleType(type)){
				if(Rest.contentTypeFailureHandler) Rest.contentTypeFailureHandler.call(type,delegate)
				else throw new RuntimeException("Expected ${type}, but response was ${delegate.type}: ${delegate}")
			}
			delegate
		}
		ClientResponse.metaClass.hasContentType={type->
			delegate.type== (type instanceof String?MediaType.valueOf(type):type)
		}
		ClientResponse.metaClass.hasCompatibleType={type->
			delegate.type.isCompatible((type instanceof String ? MediaType.valueOf(type) : type))
		}
		ClientResponse.metaClass.requireStatus={status->
			if(delegate.status!=status){
				if(Rest.failureHandler) Rest.failureHandler.call(delegate)
				else throw new RuntimeException("Expected ${status}, but response was ${delegate.status}: ${delegate}")
			}
		}
		WebResource.Builder.metaClass.leftShift<<{Map map->
			map?.each{
				delegate.header(it.key,it.value)
			}
		}
	}

	Client client = Client.create()
	/**
	 * Default accept header MediaType
	 */
	def static defaultAccept=MediaType.APPLICATION_XML_TYPE
	/**
	 * Map of default request headers
	 */
	def static defaultHeaders=[:]
	def static mock
	/**
	 * Closure that will be called if the response is not a successful status value. Argument is a ClientResponse object.
	 */
	def static failureHandler
	/**
	 * Closure that will be called if ClientResponse.requireContentType doesn't match the response. Arguments are (content type, response)
	 */
	def static contentTypeFailureHandler

	/**
	 * if true, include xml declaration in any generated xml
	 */
	def static xmlDeclaration=true

	/**
	 * Jersey client Resource
	 */
	def resource
	/**
	 * Request headers to send for any requests for this resource
	 */
	def headers=[:]
	/**
	 * Accept header value
	 */
	def accept=defaultAccept
	
	def addFilter(filter){
		client.addFilter(filter)
	}
	/**
	 * Print debug output for all requests/responses to the given PrintStream
	 */
	def debug(PrintStream out){
		addFilter(new LoggingFilter(out))
	}
    /**
     * Create a Rest client with baseUrl, path and client config
     * @param baseUrl
     * @param path
     * @param config
     */
    public Rest(String baseUrl,String path, ClientConfig config){
        if(null!=config){
            client= Client.create(config)
        }
        if (baseUrl && !path.startsWith('http')) {
            resource = client.resource(baseUrl).path(path)
        } else {
            resource = client.resource(path)
        }
    }
	/**
	 * Create a new Rest given the URL path.
	 */
	public Rest(String path){
		this(null,path,null)
	}
	private Rest(WebResource resource){
		this.resource=resource
	}
	/**
	 * Override the '+' operator to support appending a URL path to a Rest instance.
	 */
	public Rest plus(String path){
		new Rest(resource.path(path))
	}
	/**
	 * Override the getAt operator to support appending a URL path to a Rest instance and executing a GET request immediately.
	 */
	public getAt(String path){
		new Rest(resource.path(path)).get()
	}
	/**
	 * Override the putAt operator to support appending a URL path to a Rest instance and executing a POST request immediately.
	 */
	public putAt(path,content){
		new Rest(resource.path(path)).post(content)
	}
	/**
	 * Override the leftShift '&lt;&lt;' operator to support POST using XML content defined in a builder closure.
	 */
	public leftShift(Closure obj){
		post(xmlContent(obj))
	}
	private makeRequest(builder,Closure clos){
		def response=builder.with(mock?:clos)
		
		if(failureHandler && (response.status <200 || response.status>=300)){
			failureHandler(response)
		}
		response
	}

	private query(params=[:]){
		if(params){
			MultivaluedMap<String, String> qparams = new MultivaluedMapImpl();
			params.each{String k,String v->
				qparams.putSingle(k,v)
			}
			return resource.queryParams(qparams)
		}
		resource
	}
	private addHeaders(builder,headers){
		builder<<defaultHeaders
		builder<<this.headers
		builder<<headers
	}

	/**
	 * Return a map containing request header for HTTP Basic authentication
	 * @param user the username string
	 * @param pass the password string
	 */
	public static basicAuthHeader(user,pass){
		['Authorization':'Basic '+"${user}:${pass}".toString().bytes.encodeBase64().toString()]
	}
	private static makeContent(content){
		if(content instanceof Closure){
			xmlContent(content)
		}else{
			content
		}
	}
	private static xmlContent(Closure clos){
		def writer = new StringWriter()
		def xml = new MarkupBuilder(writer)
		if(xmlDeclaration){
			xml.mkp.xmlDeclaration(version:'1.0',encoding:'UTF-8')
		}
		clos.delegate=xml
		clos.call()
		writer.toString()
	}

	private build(headers,params){
		def builder= query(params).accept(accept)
		addHeaders(builder,headers)
		builder
	}

	/**
	 * GET request for this URL.
	 * @param headers request header map
	 * @param params request params map
	 */
	public ClientResponse get(headers=[:],params=[:]){
		makeRequest(build(headers,params)){
			get(ClientResponse.class);
		}
	}
	/**
	 * POST request to this URL.
	 * @param content any text content, or a closure for building XML content
	 * @param headers request header map
	 * @param params request params map
	 */
	public ClientResponse post(content,headers=[:],params=[:]){
		def value=makeContent(content)
		makeRequest(build(headers,params)){
			post(ClientResponse.class,value);
		}
	}
	/**
	 * POST request to this URL, using a builder closure to define XML content
	 * @param headers request header map
	 * @param params request params map
	 * @param clos a closure for building XML content
	 */
	public post(headers,params,Closure clos){
		def content=makeContent(clos)
		makeRequest(build(headers,params)){
			post(ClientResponse.class,content);
		}
	}
	/**
	 * POST request to this URL, using a builder closure to define XML content
	 * @param headers request header map
	 * @param clos a closure for building XML content
	 */
	public post(headers,Closure clos){
		post(headers,[:],clos)
	}
	/**
	 * POST request to this URL, using a builder closure to define XML content
	 * @param clos a closure for building XML content
	 */
	public post(Closure clos){
		post([:],[:],clos)
	}
	/**
	 * PUT request to this URL.
	 * @param content any text content, or a closure for building XML content
	 * @param headers request header map
	 * @param params request params map
	 */
	public put(content,headers=[:],params=[:]){
		def value=makeContent(content)
		makeRequest(build(headers,params)){
			put(ClientResponse.class,value);
		}
	}
	/**
	 * DELETE request for this URL.
	 * @param headers request header map
	 * @param params request params map
	 */
	public delete(headers=[:],params=[:]){
		makeRequest(build(headers,params)){
			delete(ClientResponse.class);
		}
	}
	def String toString(){
		resource.toString()
	}
}
