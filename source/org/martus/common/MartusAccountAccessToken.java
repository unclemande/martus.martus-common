/*

The Martus(tm) free, social justice documentation and
monitoring software. Copyright (C) 2014, Beneficent
Technology, Inc. (Benetech).

Martus is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either
version 2 of the License, or (at your option) any later
version with the additions and exceptions described in the
accompanying Martus license file entitled "license.txt".

It is distributed WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, including warranties of fitness of purpose or
merchantability.  See the accompanying Martus License and
GPL license for more details on the required license terms
for this software.

You should have received a copy of the GNU General Public
License along with this program; if not, write to the Free
Software Foundation, Inc., 59 Temple Place - Suite 330,
Boston, MA 02111-1307, USA.

*/
package org.martus.common;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;

import org.miradi.utils.EnhancedJsonObject;


public class MartusAccountAccessToken
{
	public static class TokenInvalidException extends Exception 
	{
	}
	
	public MartusAccountAccessToken(String newToken) throws TokenInvalidException
	{
		setToken(newToken);
	}
	
	public static MartusAccountAccessToken loadFromFile(File tokensFile) throws FileNotFoundException, IOException, TokenInvalidException
	{
		FileInputStream contactFileInputStream = new FileInputStream(tokensFile);
		DataInputStream in = new DataInputStream(contactFileInputStream);
		String data = in.readUTF();
		in.close();
		return loadFromString(data);
	}
	
	public static MartusAccountAccessToken loadFromString(String rawJsonTokenData) throws TokenInvalidException
	{
		try
		{
			EnhancedJsonObject jsonObject = new EnhancedJsonObject(rawJsonTokenData);
			EnhancedJsonObject innerObject = jsonObject.getJson(MARTUS_ACCESS_TOKEN_RESPONSE_TAG);
			return new MartusAccountAccessToken(innerObject.getString(MARTUS_ACCESS_TOKEN_JSON_TAG));
		} 
		catch (ParseException e)
		{
			MartusLogger.log("json Parse Exception in MartusAccountAccessToken loadFromString");
			throw new TokenInvalidException();
		}
	}

	public String getToken()
	{
		return token;
	}
	
	public boolean equals(Object otherObject)
	{
		if(otherObject instanceof MartusAccountAccessToken)
			return getToken().equals(((MartusAccountAccessToken)otherObject).getToken());
		return false;
	}

	public String toString()
	{
		return getToken();
	}
	
	public int hashCode()
	{
		return getToken().hashCode();
	}
	
	
	private void setToken(String newToken) throws TokenInvalidException
	{
		if(!validToken(newToken))
			throw new TokenInvalidException();
		token = newToken;
	}

	private boolean validToken(String tokenToValidate)
	{
		DammCheckDigitAlgorithm validationCheck = new DammCheckDigitAlgorithm();
		return validationCheck.validateToken(tokenToValidate);
	}

	public static final String MARTUS_ACCESS_TOKEN_JSON_TAG = "Token";
	public static final String MARTUS_ACCESS_TOKEN_CREATION_DATE_JSON_TAG = "DateCreated";
	public static final String MARTUS_ACCESS_TOKEN_RESPONSE_TAG = "MartusAccessTokenResponse";
	
	private String token;
}
